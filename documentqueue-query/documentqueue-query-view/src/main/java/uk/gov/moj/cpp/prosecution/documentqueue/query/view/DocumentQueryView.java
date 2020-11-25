package uk.gov.moj.cpp.prosecution.documentqueue.query.view;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentIdsOfCases;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentsCount;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.service.DocumentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;

public class DocumentQueryView {

    @Inject
    private DocumentService documentService;

    @Inject
    private ListToJsonArrayConverter<ScanDocument> listToJsonArrayConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public Envelope<JsonObject> getDocuments(final JsonEnvelope query) {

        final List<ScanDocument> scanDocuments = documentService.getDocuments(query);
        final JsonObject documentsPayload = buildDocumentsPayload(scanDocuments);

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


    private JsonObject buildDocumentsPayload(final List<ScanDocument> documents) {
        return createObjectBuilder()
                .add("documents", listToJsonArrayConverter.convert(documents))
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
