package uk.gov.moj.cpp.prosecution.documentqueue.query.view;

import org.apache.commons.lang3.tuple.Pair;
import uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentIdsOfCases;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentsCount;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.service.DocumentService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

public class DocumentQueryView {

    private static final String PAGE = "page";
    private static final String PAGE_SIZE = "pageSize";
    private static final String SORT_BY = "sortBy";
    private static final String ORDER_BY = "orderBy";
    private static final String SOURCE = "source";
    private static final String STATUS = "status";
    private static final int DEFAULT_CASES_PAGE_SIZE = 50;
    private static final String VENDOR_RECEIVED_DATE = "vendorReceivedDate";
    private static final String STATUS_UPDATED_DATE = "statusUpdatedDate";
    private static final String ASC = "asc";
    private static final String DESC = "desc";
    public static final String DOCUMENTS = "documents";
    public static final String TOTAL = "total";

    @Inject
    private DocumentService documentService;

    @Inject
    private ListToJsonArrayConverter<ScanDocument> listToJsonArrayConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public Envelope<JsonObject> getDocuments(final JsonEnvelope query) {

        final JsonObject payload = query.payloadAsJsonObject();

        final int pageSize = payload.getInt(PAGE_SIZE, DEFAULT_CASES_PAGE_SIZE);
        final int page = payload.getInt(PAGE, 1);

        if(page <= 0 || pageSize <= 0) {
            throw new IllegalArgumentException(format("invalid page number (%s) or page size (%s)", page, pageSize));
        }

        final String sort = getString(payload, SORT_BY).orElse(VENDOR_RECEIVED_DATE);

        if(!(sort.equalsIgnoreCase(VENDOR_RECEIVED_DATE) || sort.equalsIgnoreCase(STATUS_UPDATED_DATE))) {
            throw new IllegalArgumentException(format("invalid sort by field (%s) ", sort));
        }

        final String sortOrder = getString(payload, ORDER_BY).orElse(ASC);

        if(!(sortOrder.equalsIgnoreCase(ASC) || sortOrder.equalsIgnoreCase(DESC))) {
            throw new IllegalArgumentException(format("invalid order by  field (%s) ", sortOrder));
        }

        final int offset = pageSize * (page - 1);

        final Optional<Source> source = getString(payload, SOURCE).map(Source::valueOf);
        final Optional<Status> status = getString(payload, STATUS).map(Status::valueOf);
        final Pair<Integer, List<ScanDocument>> documentListWithTotalSize = documentService.getDocuments(source, status, sort, sortOrder, offset, pageSize);


        final List<ScanDocument> scanDocuments = documentListWithTotalSize.getRight();
        final int totalCount = documentListWithTotalSize.getLeft();

        final JsonObject documentsPayload = buildDocumentsPayload(scanDocuments, page, pageSize, totalCount, sort );

        return envelop(documentsPayload)
                .withName("documentqueue.query.documents")
                .withMetadataFrom(query);
    }

    public Envelope<DocumentsCount> getDocumentsCount(final JsonEnvelope query) {
        final DocumentsCount documentsCount = documentService.getDocumentsCount();
        return envelop(documentsCount).withName("documentqueue.query.documents-counts").withMetadataFrom(query);
    }

    public Envelope<DocumentContentView> getDocumentContent(final JsonEnvelope query) {

        final UUID documentId = UUID.fromString(query.payloadAsJsonObject().getString("documentId"));
        final Optional<DocumentContentView> optionalDocumentContentView = documentService.getDocument(documentId);

        return optionalDocumentContentView.map(documentContentView ->
                envelop(documentContentView)
                        .withName("documentqueue.query.document-content")
                        .withMetadataFrom(query))
                .orElse(envelop((DocumentContentView) null)
                        .withName("documentqueue.query.document-content")
                        .withMetadataFrom(query));
    }

    public Envelope<ScanDocument> getDocument(final JsonEnvelope query) {
        final UUID documentId = UUID.fromString(query.payloadAsJsonObject().getString("documentId"));
        final Optional<ScanDocument> optionalScanDocument = documentService.getDocumentById(documentId);

        return optionalScanDocument.map(scanDocument ->
                envelop(scanDocument)
                        .withName("documentqueue.query.get-document")
                        .withMetadataFrom(query))
                .orElse(envelop((ScanDocument) null)
                        .withName("documentqueue.query.get-document")
                        .withMetadataFrom(query));
    }

    public Envelope<DocumentIdsOfCases> getDocumentIdsOfCases(final JsonEnvelope query) {
        final List<String> urns = query
                .payloadAsJsonObject()
                .getJsonArray("urns")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(Collectors.toList());

        final Optional<DocumentIdsOfCases> documentIdsOfCasesOptional = documentService.getDocumentIdsForCases(urns);
        return documentIdsOfCasesOptional.map(documentIdsOfCases ->
                envelop(documentIdsOfCases)
                        .withName("documentqueue.query.document-ids-of-cases")
                        .withMetadataFrom(query))
                .orElse(envelop((DocumentIdsOfCases) null)
                        .withName("documentqueue.query.document-ids-of-cases")
                        .withMetadataFrom(query));
    }


    private JsonObject buildDocumentsPayload(final List<ScanDocument> documents, final int page, final int pageSize, final int totalCount, final String sort) {
        return createObjectBuilder()
                .add(DOCUMENTS, listToJsonArrayConverter.convert(documents))
                .add(TOTAL, totalCount)
                .add(PAGE, page)
                .add(PAGE_SIZE, pageSize)
                .add(SORT_BY, sort)
                .build();
    }

    public Envelope<List<Document>> getExpiredDocuments(final JsonEnvelope requestEnvelope) {
        final int documentExpiryDays = requestEnvelope.payloadAsJsonObject().getInt("documentExpiryDays");
        final List<Document> expiredDocuments = documentService.getExpiredDocuments(documentExpiryDays);

        return envelop(expiredDocuments)
                .withName("documentqueue.query.expired-documents")
                .withMetadataFrom(requestEnvelope);
    }

    public Envelope<List<Document>> getDocumentsEligibleForDeletionFromFileStore(final JsonEnvelope requestEnvelope) {
        final int documentFileServiceDeleteDays = requestEnvelope.payloadAsJsonObject().getInt("documentFileServiceDeleteDays");
        final int maxResults  = requestEnvelope.payloadAsJsonObject().getInt("maxResults");
        final List<Document> documentsToBeDeletedFromFileStore = documentService.getDocumentsEligibleForDeletionFromFileStore(documentFileServiceDeleteDays, maxResults);

        return envelop(documentsToBeDeletedFromFileStore)
                .withName("documentqueue.query.documents-eligible-for-deletion-from-fileservice")
                .withMetadataFrom(requestEnvelope);
    }

}
