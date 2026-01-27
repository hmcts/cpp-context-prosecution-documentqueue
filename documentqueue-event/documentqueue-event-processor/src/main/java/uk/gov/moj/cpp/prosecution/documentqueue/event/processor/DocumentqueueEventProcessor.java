package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.documentqueue.command.handler.RemoveDocumentFromQueue.removeDocumentFromQueue;
import static uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus.updateDocumentStatus;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDeleteExpiredDocumentsAsRequested.markDeleteExpiredDocumentsAsRequested;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDocumentDeletedFromFilestore.markDocumentDeletedFromFilestore;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDocumentsDeletedForCases.markDocumentsDeletedForCases;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument.receiveOutstandingDocument;
import static uk.gov.moj.cpp.prosecution.documentqueue.event.processor.CaseProcessor.DOCUMENTQUEUE_COMMAND_REMOVE_DOCUMENT_FROM_QUEUE;

import uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentIdsOfCases;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.model.CourtDocument;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus;
import uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesRequested;
import uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequestReceived;
import uk.gov.moj.cpp.documentqueue.event.DocumentDeleteFromFileStoreRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentLinkedToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.AttachDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDeleteExpiredDocumentsAsRequested;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDocumentDeletedFromFilestore;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDocumentsDeletedForCases;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.DocumentQueryView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
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
    private static final String DOCUMENT_QUEUE_QUERY_EXPIRED_DOCUMENTS = "documentqueue.query.expired-documents";
    private static final String DOCUMENT_QUEUE_QUERY_DELETE_FILE_SERVICE_DOCUMENTS = "documentqueue.query.documents-eligible-for-deletion-from-fileservice";
    private static final String STAGING_BULKSCAN_MARK_DOCUMENT = "stagingbulkscan.mark-as-action";
    private static final String DOCUMENT_QUEUE_QUERY_DOCUMENT_CONTENT = "documentqueue.query.document-content";
    private static final String MATERIAL_UPLOAD_FILE = "material.command.upload-file";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String PUBLIC_DOCUMENT_QUEUE_DOC_ATTACHED = "public.documentqueue.document-attached";
    private static final String RECORD_DOCUMENT_ATTACHED = "documentqueue.command.record-document-attached";
    private static final String PUBLIC_DOCUMENT_QUEUE_DOCUMENT_ALREADY_ATTACHED = "public.documentqueue.document-already-attached";
    private static final String DOCUMENT_ID = "documentId";
    private static final String DOCUMENTQUEUE_COMMAND_RECEIVE_OUTSTANDING_DOCUMENT = "documentqueue.command.receive-outstanding-document";
    private static final String DOCUMENT_QUEUE_QUERY_GET_DOCUMENT_IDS_FOR_CASES = "documentqueue.query.document-ids-of-cases";
    private static final String DOCUMENT_QUEUE_COMMAND_MARK_DOCUMENTS_DELETED_FOR_CASES = "documentqueue.command.mark-documents-deleted-for-cases";
    private static final String DOCUMENT_QUEUE_COMMAND_MARK_DOCUMENT_DELETED_FILE_STORE = "documentqueue.command.mark-document-deleted-from-file-store";
    private static final String DOCUMENT_QUEUE_COMMAND_MARK_DELETE_EXPIRED_DOCUMENTS_AS_REQUESTED = "documentqueue.command.mark-delete-expired-documents-as-requested";
    private static final String DOCUMENT_QUEUE_COMMAND_UPDATE_DOCUMENT_STATUS = "documentqueue.command.update-document-status";


    @Inject
    private Sender sender;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DocumentQueryView documentQueryView;

    @Inject
    private FileService fileService;

    @Inject
    @Value(key = "document.expiry.days", defaultValue = "28")
    private String documentExpiryDays;

    @Inject
    @Value(key = "document.fileservice.delete.days", defaultValue = "90")
    private String documentFileServiceDeleteDays;

    @Inject
    @Value(key = "document.fileservice.delete.limit", defaultValue = "300")
    private String documentFileServiceDeleteLimit;

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

    @Handles("documentqueue.event.document-linked-to-case")
    public void processDocumentLinkedToCase(final Envelope<DocumentLinkedToCase> documentLinkedToCaseEnvelope) {
        final ReceiveOutstandingDocument receiveOutstandingDocumentEnvelope = receiveOutstandingDocument().withOutstandingDocument(documentLinkedToCaseEnvelope.payload().getDocument()).build();
        sender.send(envelop(receiveOutstandingDocumentEnvelope)
                .withName(DOCUMENTQUEUE_COMMAND_RECEIVE_OUTSTANDING_DOCUMENT)
                .withMetadataFrom(documentLinkedToCaseEnvelope));
    }

    private JsonObject buildPayload(final UUID scanEnvelopeId, final UUID documentId) {
        return createObjectBuilder()
                .add("scanDocumentId", documentId.toString())
                .add("scanEnvelopeId", scanEnvelopeId.toString())
                .build();
    }

    private Optional<String> getEnvelopeIdByDocumentId(final UUID documentId) {
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName(DOCUMENT_QUEUE_QUERY_GET_DOCUMENT).build(),
                createObjectBuilder().add(DOCUMENT_ID, documentId.toString()).build());

        final Envelope<ScanDocument> scanDocumentEnvelope = documentQueryView.getDocument(requestEnvelope);
        return ofNullable(scanDocumentEnvelope.payload().getEnvelopeId()).map(UUID::toString);
    }

    @Handles("documentqueue.event.document-status-update-failed")
    public void processDocumentStatusUpdateFailed(final JsonEnvelope event) {

        if (logger.isDebugEnabled()) {
            logger.debug("public.documentqueue.document-status-update-failed {}", event.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_DOCUMENT_STATUS_UPDATE_FAILED).build(), event.payload()));
    }

    @SuppressWarnings({"squid:S2221", "squid:S3655"})
    @Handles("documentqueue.event.attach-document-requested")
    public void attachDocument(final Envelope<AttachDocument> event) {
        final AttachDocument attachDocumentPayload = event.payload();
        final UUID documentId = attachDocumentPayload.getDocumentId();

        final CourtDocument courtDocument = attachDocumentPayload.getCourtDocument();

        final JsonObject courtDocumentJson = objectToJsonObjectConverter.convert(courtDocument);

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(metadataFrom(event.metadata()).withName(DOCUMENT_QUEUE_QUERY_DOCUMENT_CONTENT),
                createObjectBuilder()
                        .add(DOCUMENT_ID, documentId.toString()));

        final Envelope<DocumentContentView> documentView = documentQueryView.getDocumentContent(requestEnvelope);

        final Optional<UUID> fileServiceId = ofNullable(documentView.payload().getFileServiceId());
        final UUID materialId = ofNullable(documentView.payload().getMaterialId()).orElse(randomUUID());

        sender.sendAsAdmin(envelopeFrom(metadataFrom(event.metadata()).withName(MATERIAL_UPLOAD_FILE).build(),
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

    @Handles("documentqueue.event.delete-documents-of-cases-requested")
    public void processDeleteDocumentsOfCasesRequested(final Envelope<DeleteDocumentsOfCasesRequested> deleteDocumentsOfCasesRequestedEnvelope) {
        final DeleteDocumentsOfCasesRequested deleteDocumentsOfCasesRequested = deleteDocumentsOfCasesRequestedEnvelope.payload();
        final boolean ptiCases = containsPtiCases(deleteDocumentsOfCasesRequested);
        final List<String> urnList = ptiCases ?
                        deleteDocumentsOfCasesRequested.getCasePTIUrns() :
                        deleteDocumentsOfCasesRequested.getCaseUrns();

        final JsonArrayBuilder urnArrayBuilder = createArrayBuilder();
        urnList.stream().forEach(urnArrayBuilder::add);

        final JsonEnvelope requestEnvelope = JsonEnvelope
                .envelopeFrom(metadataBuilder()
                                .withId(randomUUID())
                                .withName(DOCUMENT_QUEUE_QUERY_GET_DOCUMENT_IDS_FOR_CASES)
                                .build(),
                        createObjectBuilder().add("urns", urnArrayBuilder.build()).build());

        final Envelope<DocumentIdsOfCases> documentIdsOfCasesEnvelope = documentQueryView.getDocumentIdsOfCases(requestEnvelope);
        final DocumentIdsOfCases documentIdsOfCases = documentIdsOfCasesEnvelope.payload();

        final MarkDocumentsDeletedForCases receiveOutstandingDocumentPayload =
                markDocumentsDeletedForCases()
                        .withProsecutionCases(documentIdsOfCases.getProsecutionCases()).build();
        sender.send(envelop(receiveOutstandingDocumentPayload)
                .withName(DOCUMENT_QUEUE_COMMAND_MARK_DOCUMENTS_DELETED_FOR_CASES)
                .withMetadataFrom(deleteDocumentsOfCasesRequestedEnvelope));
    }

    @SuppressWarnings("squid:S2221")
    @Handles("documentqueue.event.document-delete-from-file-store-requested")
    public void processDocumentDeleteFromFileStoreRequested(final Envelope<DocumentDeleteFromFileStoreRequested>
                                                     documentDeleteFromFileStoreRequestedEnvelope) throws FileServiceException {
        final DocumentDeleteFromFileStoreRequested documentDeleteFromFileStoreRequested = documentDeleteFromFileStoreRequestedEnvelope.payload();
        // revert this try catch when the cps documents cleanup SDP is run properly
        try {
            fileService.delete(documentDeleteFromFileStoreRequested.getFileServiceId());
        } catch (Exception exception) {
            logger.info("Exception while deleting file from file service for fileServiceId:{}, documentId:{}",
                    documentDeleteFromFileStoreRequested.getFileServiceId(),
                    documentDeleteFromFileStoreRequested.getDocumentId());
        }

        final MarkDocumentDeletedFromFilestore markDocumentDeletedFromFileStore =
                markDocumentDeletedFromFilestore()
                        .withDocumentId(documentDeleteFromFileStoreRequested.getDocumentId())
                        .build();

        sender.send(envelop(markDocumentDeletedFromFileStore)
                .withName(DOCUMENT_QUEUE_COMMAND_MARK_DOCUMENT_DELETED_FILE_STORE)
                .withMetadataFrom(documentDeleteFromFileStoreRequestedEnvelope));
    }

    @Handles("documentqueue.event.delete-expired-documents-request-received")
    public void processDeleteExpiredDocumentsRequestReceived(final Envelope<DeleteExpiredDocumentsRequestReceived> envelope) {

        deleteExpiredDocuments(envelope);

        deleteDocumentFromFileService(envelope);

    }

    @SuppressWarnings("squid:S2221")
    private void deleteDocumentFromFileService(final Envelope<DeleteExpiredDocumentsRequestReceived> envelope) {
        // get the expired document ids
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(DOCUMENT_QUEUE_QUERY_DELETE_FILE_SERVICE_DOCUMENTS)
                        .build(),
                createObjectBuilder()
                        .add("documentFileServiceDeleteDays", Integer.valueOf(documentFileServiceDeleteDays))
                        .add("maxResults", Integer.valueOf(documentFileServiceDeleteLimit))
                        .build());
        final List<Document> deleteDocumentsFromFileStore = documentQueryView.getDocumentsEligibleForDeletionFromFileStore(requestEnvelope).payload();

        deleteDocumentsFromFileStore
                .forEach(e -> {
                    try {
                        fileService.delete(e.getScanDocumentId());
                        final UpdateDocumentStatus updateDocumentStatus =
                                updateDocumentStatus()
                                .withDocumentId(e.getScanDocumentId())
                                .withStatus(Status.FILE_DELETED)
                                .build();
                        sender.send(envelop(updateDocumentStatus)
                                .withName(DOCUMENT_QUEUE_COMMAND_UPDATE_DOCUMENT_STATUS)
                                .withMetadataFrom(envelope));
                    } catch (Exception exception) {
                        // we don't want to bailout as it is just a cleanup
                        logger.info("Document not deleted from file store: {}", e.getScanDocumentId(), exception);
                    }
                });
    }

    private void deleteExpiredDocuments(final Envelope<DeleteExpiredDocumentsRequestReceived> deleteExpiredDocumentsRequestReceivedEnvelope) {
        // get the expired document ids
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(DOCUMENT_QUEUE_QUERY_EXPIRED_DOCUMENTS)
                        .build(),
                createObjectBuilder()
                        .add("documentExpiryDays", Integer.valueOf(documentExpiryDays))
                        .build());
        final List<Document> expiredDocumentIds = documentQueryView.getExpiredDocuments(requestEnvelope).payload();

        // fire command to delete the document
        final List<UUID> documentList = new ArrayList<>();
        expiredDocumentIds
                .forEach(e -> {
                    documentList.add(e.getScanDocumentId());
                    // payload
                    sender.send(envelop(removeDocumentFromQueue()
                            .withDocumentId(e.getScanDocumentId())
                            .build())
                            .withName(DOCUMENTQUEUE_COMMAND_REMOVE_DOCUMENT_FROM_QUEUE)
                            .withMetadataFrom(deleteExpiredDocumentsRequestReceivedEnvelope));
                });

        // finally raise a command to mark the documents as requested
        final MarkDeleteExpiredDocumentsAsRequested markDeleteExpiredDocumentsAsRequested =
                markDeleteExpiredDocumentsAsRequested()
                        .withDocumentIds(documentList)
                        .build();

        sender.send(envelop(markDeleteExpiredDocumentsAsRequested)
                .withName(DOCUMENT_QUEUE_COMMAND_MARK_DELETE_EXPIRED_DOCUMENTS_AS_REQUESTED)
                .withMetadataFrom(deleteExpiredDocumentsRequestReceivedEnvelope));
    }

    private JsonObject enrichMaterialId(final JsonObject source, String materialId) {
        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
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

    private boolean containsPtiCases(DeleteDocumentsOfCasesRequested deleteDocumentsOfCasesRequested) {
        return deleteDocumentsOfCasesRequested.getCasePTIUrns() != null;
    }
}