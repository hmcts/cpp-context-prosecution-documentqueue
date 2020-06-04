package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.prosecution.documentqueue.domain.model.CourtDocument;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.AttachDocument;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

@SuppressWarnings({"squid:S00107", "squid:S1166"})
@ServiceComponent(EVENT_PROCESSOR)
public class DocumentqueueEventProcessor {

    private static final String PUBLIC_DOCUMENT_QUEUE_DOC_STATUS_UPDATED = "public.documentqueue.document-status-updated";
    private static final String PUBLIC_DOCUMENT_STATUS_UPDATE_FAILED = "public.documentqueue.event.document-status-update-failed";
    private static final String DOCUMENT_QUEUE_QUERY_GET_DOCUMENT = "documentqueue.query.get-document";
    private static final String STAGING_BULKSCAN_MARK_DOCUMENT = "stagingbulkscan.command.mark-as-action";
    private static final String DOCUMENT_QUEUE_QUERY_DOCUMENT_CONTENT = "documentqueue.query.document-content";
    private static final String MATERIAL_UPLOAD_FILE = "material.command.upload-file";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.command.add-court-document";
    private static final String PUBLIC_DOCUMENT_QUEUE_DOC_ATTACHED = "public.documentqueue.document-attached";
    private static final String RECORD_DOCUMENT_ATTACHED = "documentqueue.command.record-document-attached";
    private static final String PUBLIC_DOCUMENT_QUEUE_DOCUMENT_ALREADY_ATTACHED ="public.documentqueue.document-already-attached";
    private static final String DOCUMENT_ID = "documentId";

    @Inject
    private Sender sender;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private Requester requester;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


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
        final UUID documentId = UUID.fromString(event.payloadAsJsonObject().getString(DOCUMENT_ID));
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
                createObjectBuilder().add(DOCUMENT_ID, documentId.toString()).build());

        final JsonEnvelope scanDocumentEnvelope = requester.request(requestEnvelope);
        return ofNullable(scanDocumentEnvelope.payloadAsJsonObject().getString("envelopeId", null));
    }

    @Handles("documentqueue.event.document-status-update-failed")
    public void processDocumentStatusUpdateFailed(final JsonEnvelope event) {

        if (logger.isDebugEnabled()) {
            logger.debug("public.documentqueue.document-status-update-failed {}", event.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_DOCUMENT_STATUS_UPDATE_FAILED).build(), event.payload()));
    }

    @SuppressWarnings({"squid:S2221","squid:S3655"})
    @Handles("documentqueue.event.attach-document-requested")
    public void attachDocument(final Envelope<AttachDocument> event) {
        final AttachDocument attachDocumentPayload = event.payload();
        final UUID documentId = attachDocumentPayload.getDocumentId();

        final CourtDocument courtDocument = attachDocumentPayload.getCourtDocument();

        final JsonObject courtDocumentJson = objectToJsonObjectConverter.convert(courtDocument);

        final Envelope<DocumentContentView> documentView = requester.request(envelopeFrom(metadataFrom(event.metadata()).withName(DOCUMENT_QUEUE_QUERY_DOCUMENT_CONTENT).build(),
                createObjectBuilder()
                        .add(DOCUMENT_ID, documentId.toString())
                        .build()), DocumentContentView.class);

        final Optional<UUID> fileServiceId = ofNullable(documentView.payload().getFileServiceId());
        final UUID materialId = ofNullable(documentView.payload().getMaterialId()).orElse(randomUUID());

            sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(MATERIAL_UPLOAD_FILE).build(),
                    createObjectBuilder()
                            .add("materialId", materialId.toString())
                            .add("fileServiceId", fileServiceId.get().toString())
                            .build()));

        sender.send(envelop(createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("courtDocument", enrichMaterialId(courtDocumentJson, materialId.toString()))
                .build())
                .withName(PROGRESSION_ADD_COURT_DOCUMENT)
                .withMetadataFrom(event));

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(RECORD_DOCUMENT_ATTACHED).build(),
                createObjectBuilder()
                        .add(DOCUMENT_ID, documentId.toString())
                        .build()));

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_DOCUMENT_QUEUE_DOC_ATTACHED).build(),
                createObjectBuilder()
                        .add(DOCUMENT_ID, documentId.toString())
                        .build()));

    }

    @Handles("documentqueue.event.document-already-attached")
    public void documentAlreadyAttached(final JsonEnvelope event) {
        if (logger.isDebugEnabled()) {
            logger.debug("public.documentqueue.event.document-already-attached {}", event.toObfuscatedDebugString());
        }
        sender.send(envelop(event.payload())
                .withName(PUBLIC_DOCUMENT_QUEUE_DOCUMENT_ALREADY_ATTACHED)
                .withMetadataFrom(event));
    }

    private JsonObject enrichMaterialId(final JsonObject source, String materialId) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        source.entrySet().forEach(e -> {
                    if ("materials".equalsIgnoreCase(e.getKey())) {
                        final JsonArray materials = (JsonArray) e.getValue();
                        final JsonObject material = materials.getJsonObject(0);
                        builder.add(e.getKey(), createArrayBuilder().add(createObjectBuilder()
                                .add("id", materialId)
                                .add("receivedDateTime", material.getString("receivedDateTime"))
                                .build()));
                    } else {
                        builder.add(e.getKey(), e.getValue());
                    }
                });

        return builder.build();
    }
}