package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

@SuppressWarnings({"squid:S00107", "squid:S1166"})
@ServiceComponent(EVENT_PROCESSOR)
public class DocumentqueueEventProcessor {

    private static final String PUBLIC_DOCUMENT_QUEUE_DOC_STATUS_UPDATED = "public.documentqueue.document-status-updated";
    private static final String PUBLIC_DOCUMENT_STATUS_UPDATE_FAILED = "public.documentqueue.event.document-status-update-failed";
    private static final String DOCUMENT_QUEUE_QUERY_GET_DOCUMENT = "documentqueue.query.get-document";
    private static final String STAGING_BULKSCAN_MARK_DOCUMENT = "stagingbulkscan.command.mark-as-action";


    @Inject
    private Sender sender;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private Requester requester;


    @Handles("documentqueue.event.document-status-updated")
    public void processDocumentStatusUpdated(final JsonEnvelope event) {
        if (logger.isDebugEnabled()) {
            logger.debug("public.documentqueue.document-status-updated {}", event.toObfuscatedDebugString());
        }
        sender.send(envelop(event.payload())
                .withName(PUBLIC_DOCUMENT_QUEUE_DOC_STATUS_UPDATED)
                .withMetadataFrom(event));
    }

    @Handles("documentqueue.event.document-marked-completed")
    public void processDocumentMarkedCompleted(final JsonEnvelope event) {
        final UUID documentId = UUID.fromString(event.payloadAsJsonObject().getString("documentId"));
        getEnvelopeIdByDocumentId(documentId).map(x -> buildPayload(fromString(x), documentId)).ifPresent(x -> sender.sendAsAdmin(envelopeFrom(metadataBuilder().withId(randomUUID()).withName(STAGING_BULKSCAN_MARK_DOCUMENT).build(), x)));
    }

    private JsonObject buildPayload(final UUID scanEnvelopeId, final UUID documentId) {
        return createObjectBuilder()
                .add("scanDocumentId", documentId.toString())
                .add("scanEnvelopeId", scanEnvelopeId.toString())
                .build();
    }

    private Optional<String> getEnvelopeIdByDocumentId(final UUID documentId) {
        final Envelope<JsonObject> requestEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(DOCUMENT_QUEUE_QUERY_GET_DOCUMENT).build(),
                createObjectBuilder().add("documentId", documentId.toString()).build());

        final JsonEnvelope scanDocumentEnvelope = requester.request(requestEnvelope);
        return Optional.ofNullable(scanDocumentEnvelope.payloadAsJsonObject().getString("envelopeId", null));
    }

    @Handles("documentqueue.event.document-status-update-failed")
    public void processDocumentStatusUpdateFailed(final JsonEnvelope event) {

        if (logger.isDebugEnabled()) {
            logger.debug("public.documentqueue.document-status-update-failed {}", event.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_DOCUMENT_STATUS_UPDATE_FAILED).build(), event.payload()));
    }
}