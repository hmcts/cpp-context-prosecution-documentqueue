package uk.gov.moj.cpp.prosecution.documentqueue.query.view.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentIdsOfCases;
import uk.gov.justice.cpp.prosecution.documentqueue.domain.ProsecutionCase;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.prosecution.documentqueue.domain.model.Applications;
import uk.gov.justice.prosecution.documentqueue.domain.model.Completed;
import uk.gov.justice.prosecution.documentqueue.domain.model.Correspondence;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentsCount;
import uk.gov.justice.prosecution.documentqueue.domain.model.InProgress;
import uk.gov.justice.prosecution.documentqueue.domain.model.Other;
import uk.gov.justice.prosecution.documentqueue.domain.model.Outstanding;
import uk.gov.justice.prosecution.documentqueue.domain.model.Pleas;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.prosecution.documentqueue.domain.model.Total;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.mapping.DocumentCountMapping;
import uk.gov.moj.cpp.prosecution.documentqueue.persistence.DocumentQueueRepository;
import uk.gov.moj.cpp.prosecution.documentqueue.persistence.DocumentRepository;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.converter.DocumentConverter;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentIdsOfCases.documentIdsOfCases;

public class DocumentService {

    @Inject
    private DocumentConverter documentConverter;

    @Inject
    private DocumentRepository documentRepository;

    @Inject
    private DocumentQueueRepository documentQueueRepository;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Pair<Integer,List<ScanDocument>> getDocuments(final Optional<Source> source, final Optional<Status> status, final String sort, final String sortOrder, final int offset, final int pageSize) {

        final Pair<Integer, List<Document>> documentListWithTotalSize = documentQueueRepository.getDocumentList(source, status, sort, sortOrder, offset, pageSize);
        final Integer totalCount = documentListWithTotalSize.getLeft();
        final List<Document> documents = documentListWithTotalSize.getRight();
        return Pair.of(totalCount, documentConverter.convertToScanDocuments(documents));
    }

    @SuppressWarnings("squid:S2629")
    public Optional<DocumentContentView> getDocument(final UUID documentId) {
        final Document document = documentRepository.findBy(documentId);

        if (document != null) {
            return of(documentConverter.convertToDocumentContentView(document));
        }

        logger.info(format("No document found in document table with id '%s'", documentId));

        return empty();
    }

    @SuppressWarnings("squid:S2629")
    public Optional<ScanDocument> getDocumentById(final UUID documentId) {
        final Document document = documentRepository.findBy(documentId);

        if (document != null) {
            return of(documentConverter.convertToScanDocument(document));
        }

        logger.info(format("No document found in document table with id '%s'", documentId));
        return empty();
    }

    public DocumentsCount getDocumentsCount() {
        final List<DocumentCountMapping> documentCountMapping = documentRepository.getDocumentCount();
        final DocumentsCount.Builder builder = new DocumentsCount.Builder();

        withCompletedCounts(documentCountMapping, builder);

        withInProgressCounts(documentCountMapping, builder);

        withOutstandingCounts(documentCountMapping, builder);

        return builder.build();
    }

    /**
     * @param urns: this can refer either to the caseUrns or the casePTIUrns
     * @return
     */
    public Optional<DocumentIdsOfCases> getDocumentIdsForCases(final List<String> urns) {
        final List<Document> documents = documentRepository.findByCaseUrnInOrCasePTIUrnInOrderByCaseUrnAsc(urns);
        final List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();

        ProsecutionCase.Builder prosecutionCaseBuilder = ProsecutionCase.prosecutionCase();
        String previousCaseUrn = null;
        List<UUID> documentIds  = new ArrayList<>();

        if (documents == null || documents.isEmpty()) {
            return Optional.of(
                    documentIdsOfCases()
                            .withProsecutionCases(prosecutionCaseList)
                            .build());
        }

        for (final Document document : documents) {
            if (!StringUtils.equals(previousCaseUrn, getUrn(document))) {
                if (previousCaseUrn != null) {
                    buildProsecutionCase(prosecutionCaseList, prosecutionCaseBuilder, documentIds);
                    // reinitialize after the build
                    documentIds = new ArrayList<>();
                    prosecutionCaseBuilder = ProsecutionCase.prosecutionCase();
                }

                prosecutionCaseBuilder
                        .withCaseUrn(document.getCaseUrn())
                        .withCasePTIUrn(document.getCasePTIUrn())
                        .withCaseId(document.getCaseId());

                previousCaseUrn = getUrn(document);
            }

            documentIds.add(document.getScanDocumentId());
        }

        buildProsecutionCase(prosecutionCaseList, prosecutionCaseBuilder, documentIds);

        return Optional.of(documentIdsOfCases().withProsecutionCases(prosecutionCaseList).build());
    }

    public List<Document> getExpiredDocuments(final int documentExpiryDays) {
        return documentRepository.getExpiredDocuments(documentExpiryDays);
    }

    public List<Document> getDocumentsEligibleForDeletionFromFileStore(final int days, final int maxResults) {
        return documentRepository.getDocumentsEligibleForDeletionFromFileStore(days, maxResults);
    }

    private void buildProsecutionCase(final List<ProsecutionCase> prosecutionCaseList, final ProsecutionCase.Builder prosecutionCaseBuilder, final List<UUID> documentIds) {
        if(prosecutionCaseBuilder != null) {
            prosecutionCaseBuilder.withDocumentIds(documentIds);
            prosecutionCaseList.add(prosecutionCaseBuilder.build());
        }
    }

    private String getUrn(final Document document) {
        return document.getCasePTIUrn() != null ? document.getCasePTIUrn() : document.getCaseUrn();
    }

    private void withOutstandingCounts(final List<DocumentCountMapping> documentCountMapping, final DocumentsCount.Builder builder) {
        final List<DocumentCountMapping> outstandingDocuments = documentCountMapping.stream().filter(statusFilter(Status.OUTSTANDING)).collect(Collectors.toList());
        builder.withOutstanding(new Outstanding.Builder()
                .withTotalCount(outstandingDocuments.stream().mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .withPleas(getPleasCount(outstandingDocuments))
                .withApplications(getApplicationsCount(outstandingDocuments))
                .withCorrespondence(getCorrespondenceCount(outstandingDocuments))
                .withOther(getOthersCount(outstandingDocuments))
                .withTotal(getTotalCount(outstandingDocuments))
                .build()
        );
    }

    private void withInProgressCounts(final List<DocumentCountMapping> documentCountMapping, final DocumentsCount.Builder builder) {
        final List<DocumentCountMapping> inProgressDocuments = documentCountMapping.stream().filter(statusFilter(Status.IN_PROGRESS)).collect(Collectors.toList());
        builder.withInProgress(new InProgress.Builder()
                .withTotalCount(inProgressDocuments.stream().mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .withPleas(getPleasCount(inProgressDocuments))
                .withApplications(getApplicationsCount(inProgressDocuments))
                .withCorrespondence(getCorrespondenceCount(inProgressDocuments))
                .withOther(getOthersCount(inProgressDocuments))
                .withTotal(getTotalCount(inProgressDocuments))
                .build()
        );
    }

    private void withCompletedCounts(final List<DocumentCountMapping> documentCountMapping, final DocumentsCount.Builder builder) {
        final List<DocumentCountMapping> completedDocuments = documentCountMapping.stream().filter(statusFilter(Status.COMPLETED)).collect(Collectors.toList());
        builder.withCompleted(new Completed.Builder()
                .withTotalCount(completedDocuments.stream().mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .withPleas(getPleasCount(completedDocuments))
                .withApplications(getApplicationsCount(completedDocuments))
                .withCorrespondence(getCorrespondenceCount(completedDocuments))
                .withOther(getOthersCount(completedDocuments))
                .withTotal(getTotalCount(completedDocuments))
                .build()
        );
    }

    private Predicate<DocumentCountMapping> statusFilter(Status status) {
        return documentCountMapping -> status == documentCountMapping.getStatus();
    }

    private Predicate<DocumentCountMapping> typeFilter(Type type) {
        return documentCountMapping -> type == documentCountMapping.getType();
    }

    private Predicate<DocumentCountMapping> sourceFilter(Source source) {
        return documentCountMapping -> source == documentCountMapping.getSource();
    }

    private Pleas getPleasCount(final List<DocumentCountMapping> documents) {
        return new Pleas.Builder()
                .withVolume(documents.stream().filter(typeFilter(Type.PLEA)).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .withBulkScan(documents.stream().filter(typeFilter(Type.PLEA).and(sourceFilter(Source.BULKSCAN))).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .build();
    }

    private Applications getApplicationsCount(final List<DocumentCountMapping> documents) {
        return new Applications.Builder()
                .withVolume(documents.stream().filter(typeFilter(Type.APPLICATION)).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .withBulkScan(documents.stream().filter(typeFilter(Type.APPLICATION).and(sourceFilter(Source.BULKSCAN))).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .build();
    }

    private Correspondence getCorrespondenceCount(final List<DocumentCountMapping> documents) {
        return new Correspondence.Builder()
                .withVolume(documents.stream().filter(typeFilter(Type.CORRESPONDENCE)).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .withBulkScan(documents.stream().filter(typeFilter(Type.CORRESPONDENCE).and(sourceFilter(Source.BULKSCAN))).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .build();
    }

    private Other getOthersCount(final List<DocumentCountMapping> documents) {
        return new Other.Builder()
                .withVolume(documents.stream().filter(typeFilter(Type.OTHER)).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .withBulkScan(documents.stream().filter(typeFilter(Type.OTHER).and(sourceFilter(Source.BULKSCAN))).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .build();
    }

    private Total getTotalCount(final List<DocumentCountMapping> documents) {
        return new Total.Builder()
                .withVolume(documents.stream().mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .withBulkScan(documents.stream().filter(sourceFilter(Source.BULKSCAN)).mapToLong(DocumentCountMapping::getCount).mapToInt(Math::toIntExact).sum())
                .build();
    }
}
