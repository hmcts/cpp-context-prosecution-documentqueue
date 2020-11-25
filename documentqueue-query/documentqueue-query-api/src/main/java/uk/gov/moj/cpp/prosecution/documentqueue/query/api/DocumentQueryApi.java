package uk.gov.moj.cpp.prosecution.documentqueue.query.api;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentsCount;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.DocumentContent;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.DocumentContentService;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.GetDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.DocumentQueryView;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(QUERY_API)
public class DocumentQueryApi {

    @Inject
    private DocumentQueryView documentQueryView;

    @Inject
    private DocumentContentService documentContentService;

    @Handles("documentqueue.query.documents")
    public Envelope<JsonObject> queryDocuments(final JsonEnvelope query) {
        return documentQueryView.getDocuments(query);
    }

    @Handles("documentqueue.query.documents-counts")
    public Envelope<DocumentsCount> queryDocumentCounts(final JsonEnvelope query) {
        return documentQueryView.getDocumentsCount(query);
    }

    @Handles("documentqueue.query.document-content")
    public Envelope<DocumentContent> getDocumentContent(final JsonEnvelope query) {

        final Envelope<DocumentContentView> documentContentEnvelope = documentQueryView.getDocumentContent(query);

        final DocumentContentView payload = documentContentEnvelope.payload();

        if (payload != null) {
            final DocumentContent documentContent = documentContentService.getDocumentContent(payload);

            return envelop(documentContent)
                    .withName("documentqueue.query.document-content")
                    .withMetadataFrom(query);
        }

        return envelop((DocumentContent) null)
                .withName("documentqueue.query.document-content")
                .withMetadataFrom(query);
    }

    @Handles("documentqueue.query.get-document")
    public Envelope<GetDocument> getDocument(final JsonEnvelope query) {
        final Envelope<ScanDocument> documentEnvelope = documentQueryView.getDocument(query);
        final ScanDocument payload = documentEnvelope.payload();

        if (payload != null) {
            final GetDocument getDocument = documentContentService.getDocument(payload);

            return envelop(getDocument)
                    .withName("documentqueue.query.get-document")
                    .withMetadataFrom(query);
        }

        return envelop((GetDocument) null)
                .withName("documentqueue.query.get-document")
                .withMetadataFrom(query);
    }
}