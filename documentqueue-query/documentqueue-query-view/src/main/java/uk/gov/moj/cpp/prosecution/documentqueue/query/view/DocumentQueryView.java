package uk.gov.moj.cpp.prosecution.documentqueue.query.view;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.prosecution.documentqueue.domain.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentsCount;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.service.DocumentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(QUERY_VIEW)
public class DocumentQueryView {

    @Inject
    private DocumentService documentService;

    @Inject
    private ListToJsonArrayConverter<ScanDocument> listToJsonArrayConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("documentqueue.query.documents")
    public Envelope<JsonObject> getDocuments(final JsonEnvelope query) {

        final List<ScanDocument> scanDocuments = documentService.getDocuments(query);
        final JsonObject documentsPayload = buildDocumentsPayload(scanDocuments);

        return envelop(documentsPayload)
                .withName("documentqueue.query.documents")
                .withMetadataFrom(query);
    }

    @Handles("documentqueue.query.documents-counts")
    public Envelope<DocumentsCount> getDocumentsCount(final JsonEnvelope query) {
        final DocumentsCount documentsCount = documentService.getDocumentsCount();
        return envelop(documentsCount).withName("documentqueue.query.documents-counts").withMetadataFrom(query);
    }

    @Handles("documentqueue.query.document-content")
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

    @Handles("documentqueue.query.get-document")
    public Envelope<ScanDocument> getDocument(final JsonEnvelope query) {
        final UUID documentId = UUID.fromString(query.payloadAsJsonObject().getString("documentId"));
        final ScanDocument scanDocument = documentService.getDocumentById(documentId);
        return envelop(scanDocument)
                .withName("documentqueue.query.get-document")
                .withMetadataFrom(query);
    }


    private JsonObject buildDocumentsPayload(final List<ScanDocument> documents) {
        return createObjectBuilder()
                .add("documents", listToJsonArrayConverter.convert(documents))
                .build();
    }
}
