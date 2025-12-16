package uk.gov.moj.cpp.prosecution.documentqueue.event.converter;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.prosecution.documentqueue.domain.Document.document;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase.*;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata.ReferenceDataService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentReviewRequired;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class ReviewDocumentConverter {

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private SystemUserProvider systemUserProvider;

    public LinkDocumentToCase asLinkDocumentToCase(final DocumentReviewRequired documentReviewRequired, final String fileName) {

        return linkDocumentToCase().withDocument(
                document()
                        .withSource(Source.valueOf(documentReviewRequired.getSource()))
                        .withNotes(documentReviewRequired.getErrorCodes().stream().collect(Collectors.joining(",")))
                        .withFileName(fileName)
                        .withFileServiceId(documentReviewRequired.getFileStoreId())
                        .withReceivedDateTime(documentReviewRequired.getReceivedDateTime())
                        .withScanDocumentId(documentReviewRequired.getFileStoreId())
                        .withExternalDocumentId(documentReviewRequired.getCmsDocumentId())
                        .withType(Type.valueOf(documentReviewRequired.getDocumentType().toString()))
                        .withStatus(Status.OUTSTANDING)
                        .withCasePTIUrn(urnFromCaseId(documentReviewRequired.getCaseId()))
                        .withProsecutorAuthorityCode(getProsecutorAuthorityCode(documentReviewRequired.getProsecutingAuthority()))
                        .withDocumentName(documentReviewRequired.getCmsDocumentId())
                        .withVendorReceivedDate(documentReviewRequired.getReceivedDateTime())
                        .withCaseId(documentReviewRequired.getCaseId())
                        .build()).build();
    }

    public LinkDocumentToCase asLinkDocumentToCase(final uk.gov.moj.cps.progression.domain.event.DocumentReviewRequired documentReviewRequired,
                                                   final String fileName) {
        return linkDocumentToCase().withDocument(
                document()
                        .withNotes(documentReviewRequired.getCode().get(0))
                        .withScanDocumentId(documentReviewRequired.getDocumentId())
                        .withDocumentName(documentReviewRequired.getDocumentName())
                        .withType(getDocumentType(documentReviewRequired.getDocumentType()))
                        .withMaterialId(documentReviewRequired.getMaterialId())
                        .withProsecutorAuthorityCode(documentReviewRequired.getProsecutingAuthority())
                        .withReceivedDateTime(documentReviewRequired.getReceivedDateTime())
                        .withSource(documentReviewRequired.getSource())
                        .withCaseUrn(documentReviewRequired.getUrn())
                        .withStatus(Status.OUTSTANDING)
                        .withFileName(fileName)
                        .withVendorReceivedDate(documentReviewRequired.getReceivedDateTime())
                        .withCaseId(documentReviewRequired.getCaseId())
                        .build()).build();
    }

    @SuppressWarnings({"squid:S3655"})
    private String urnFromCaseId(final UUID caseId) {
        final UUID userId = systemUserProvider.getContextSystemUserId().get();
        final Optional<SystemIdMapping> systemIdMapping = systemIdMapperClient.findBy(caseId, "CASE_FILE_ID", userId);
        if (systemIdMapping.isPresent()) {
            return systemIdMapping.get().getSourceId();
        }
        return null;
    }

    private String getProsecutorAuthorityCode(final String prosecutingAuthorityOUCode) {
        return isBlank(prosecutingAuthorityOUCode) ? "CPS" : referenceDataService.getAuthorityShortNameForOUCode(prosecutingAuthorityOUCode);
    }

    private Type getDocumentType(final String documentType) {
        if (isBlank(documentType)) {
            return null;
        }

        for (final Type type : Type.values()) {
            if (documentType.toLowerCase().contains(type.toString().toLowerCase())) {
                return type;
            }
        }

        return Type.OTHER;
    }
}
