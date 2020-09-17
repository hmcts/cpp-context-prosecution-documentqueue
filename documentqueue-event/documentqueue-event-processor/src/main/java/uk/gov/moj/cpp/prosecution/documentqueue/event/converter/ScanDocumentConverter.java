package uk.gov.moj.cpp.prosecution.documentqueue.event.converter;

import static uk.gov.justice.prosecution.documentqueue.domain.Document.document;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase.linkDocumentToCase;
import static uk.gov.moj.cpp.prosecution.documentqueue.event.util.ProsecutorCaseReferenceUtil.getProsecutorCaseReference;

import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeDocument;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.service.SystemIdMapperService;

import javax.inject.Inject;
import java.util.UUID;


public class ScanDocumentConverter {

    @Inject
    private SystemIdMapperService systemIdMapperService;

    public LinkDocumentToCase asLinkDocumentToCase(final ScanDocument scanDocument, final UUID scanEnvelopeId) {

        return linkDocumentToCase().withDocument(
                document()
                        .withEnvelopeId(scanEnvelopeId.toString())
                        .withActionedBy(scanDocument.getActionedBy())
                        .withCasePTIUrn(scanDocument.getCasePTIUrn())
                        .withCaseUrn(scanDocument.getCaseUrn())
                        .withDocumentControlNumber(scanDocument.getDocumentControlNumber())
                        .withDocumentName(scanDocument.getDocumentName())
                        .withFileName(scanDocument.getFileName())
                        .withManualIntervention(scanDocument.getManualIntervention())
                        .withNotes(scanDocument.getNotes())
                        .withProsecutorAuthorityCode(scanDocument.getProsecutorAuthorityCode())
                        .withProsecutorAuthorityId(scanDocument.getProsecutorAuthorityId())
                        .withScanDocumentId(scanDocument.getScanDocumentId())
                        .withScanningDate(scanDocument.getScanningDate())
                        .withStatusUpdatedDate(scanDocument.getStatusUpdatedDate())
                        .withVendorReceivedDate(scanDocument.getVendorReceivedDate())
                        .withSource(Source.BULKSCAN)
                        .withZipFileName(scanDocument.getZipFileName())
                        .withType(getDocumentType(scanDocument.getDocumentName()))
                        .withStatus(OUTSTANDING)
                        .withCaseId(getCaseId(scanDocument.getProsecutorAuthorityId(), scanDocument.getCaseUrn(), scanDocument.getCasePTIUrn()))
                        .build()).build();
    }

    public LinkDocumentToCase asLinkDocumentToCase(final ScanEnvelopeDocument document) {

        return linkDocumentToCase().withDocument(
                document()
                        .withActionedBy(document.getActionedBy())
                        .withCasePTIUrn(document.getCasePTIUrn())
                        .withCaseUrn(document.getCaseUrn())
                        .withFileName(document.getDocumentFileName())
                        .withDocumentName(document.getDocumentName())
                        .withNotes(document.getNotes())
                        .withProsecutorAuthorityCode(document.getProsecutorAuthorityCode())
                        .withProsecutorAuthorityId(document.getProsecutorAuthorityId())
                        .withScanDocumentId(document.getId())
                        .withStatusUpdatedDate(document.getStatusUpdatedDate())
                        .withVendorReceivedDate(document.getVendorReceivedDate())
                        .withSource(Source.BULKSCAN)
                        .withZipFileName(document.getZipFileName())
                        .withEnvelopeId(document.getScanEnvelopeId().toString())
                        .withType(getDocumentType(document.getDocumentName()))
                        .withStatus(OUTSTANDING)
                        .withCaseId(getCaseId(document.getProsecutorAuthorityId(), document.getCaseUrn(), document.getCasePTIUrn()))
                        .withType(getDocumentType(document.getDocumentName()))
                        .withStatusCode(document.getStatusCode())
                        .build()).build();
    }

    private UUID getCaseId(final String prosecutorAuthorityId, final String caseUrn, final String ptiUrn) {
        final String urn = StringUtils.isBlank(caseUrn) ? ptiUrn : caseUrn;
        final String prosecutorIdWithDocument = StringUtils.isBlank(caseUrn) ? null : prosecutorAuthorityId;
        final String prosecutorCaseReference = getProsecutorCaseReference(
                prosecutorIdWithDocument,
                urn);
        return systemIdMapperService.getCppCaseIdFor(prosecutorCaseReference);
    }
    private Type getDocumentType(String documentName) {
        if (documentName.contains("Single Justice Procedure Notice - Plea (Multiple)")  || documentName.contains("Single Justice Procedure Notice - Plea (Single)")) {
            return Type.PLEA;
        }

        if (documentName.contains("Application for extension of precharge bail")) {
            return Type.APPLICATION;
        }

        if (documentName.contains("return to sender envelope")) {
            return Type.CORRESPONDENCE;
        } else {
            return Type.OTHER;
        }
    }
}
