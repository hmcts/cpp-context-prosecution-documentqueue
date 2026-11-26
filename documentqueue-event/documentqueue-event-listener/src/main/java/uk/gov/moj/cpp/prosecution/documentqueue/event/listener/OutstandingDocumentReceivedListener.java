package uk.gov.moj.cpp.prosecution.documentqueue.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.DocumentConverter;
import uk.gov.moj.cpp.prosecution.documentqueue.event.service.DocumentService;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class OutstandingDocumentReceivedListener {

    @Inject
    private DocumentService documentService;

    @Inject
    private DocumentConverter documentConverter;

    @Handles("documentqueue.event.outstanding-document-received")
    public void handleOutstandingDocumentReceived(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final Optional<ZonedDateTime> eventDateTime = envelope.metadata().createdAt();

        final JsonObject outstandingDocument = payload.getJsonObject("outstandingDocument");

        final Document document = documentConverter.convertDocument(outstandingDocument, eventDateTime);
        documentService.saveDocument(document);
    }

}
