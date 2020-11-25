package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.documentqueue.command.handler.RemoveDocumentFromQueue.removeDocumentFromQueue;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.documentqueue.command.handler.RecordCaseStatus;
import uk.gov.moj.cpp.documentqueue.command.handler.RemoveDocumentFromQueue;
import uk.gov.moj.cpp.prosecution.documentqueue.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata.ReferenceDataServiceInterface;
import uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProcessor.class);

    public static final String DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS = "documentqueue.command.record-case-status";
    public static final String DOCUMENTQUEUE_COMMAND_REMOVE_DOCUMENT_FROM_QUEUE = "documentqueue.command.remove-document-from-queue";

    @Inject
    private Sender sender;

    @Inject
    private ReferenceDataServiceInterface referenceDataServiceInterface;

    @Handles("public.progression.events.case-or-application-ejected")
    public void processCaseRejectedPublicEvent(final JsonEnvelope caseOrApplicationEjected) {
        final String prosecutionCaseId = caseOrApplicationEjected.payloadAsJsonObject().getString("prosecutionCaseId", "");
        if (prosecutionCaseId.isEmpty()) {
            LOGGER.info("prosecutionCaseId not present so case not ejected");
        } else {
            // payload
            final RecordCaseStatus recordCaseStatus =
                    RecordCaseStatus
                            .recordCaseStatus()
                            .withStatus(CaseStatus.EJECTED)
                            .withCaseId(UUID.fromString(prosecutionCaseId))
                            .build();
            // envelope
            final Envelope<RecordCaseStatus> envelope =
                    envelop(recordCaseStatus)
                            .withName(DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS)
                            .withMetadataFrom(caseOrApplicationEjected);
            sender.send(envelope);
        }

    }

    @Handles("public.stagingprosecutorsspi.event.prosecution-case-filtered")
    public void processCaseFilteredPublicEvent(final JsonEnvelope caseOrApplicationFiltered) {
        final String caseId = caseOrApplicationFiltered
                .payloadAsJsonObject()
                .getString("caseId", "");

        if (caseId.isEmpty()) {
            LOGGER.info("caseId not present so case not filtered");
        } else {
            // payload
            final RecordCaseStatus recordCaseStatus =
                    RecordCaseStatus
                            .recordCaseStatus()
                            .withStatus(CaseStatus.FILTERED)
                            .withCaseId(UUID.fromString(caseId))
                            .build();
            // envelope
            final Envelope<RecordCaseStatus> envelope =
                    envelop(recordCaseStatus)
                            .withName(DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS)
                            .withMetadataFrom(caseOrApplicationFiltered);
            sender.send(envelope);
        }

    }

    @Handles("public.prosecutioncasefile.prosecution-submission-succeeded")
    public void processSubmissionSucceededEvent(final JsonEnvelope caseOrApplicationFiltered) {
        processSubmissionSucceeded(caseOrApplicationFiltered);

    }

    @Handles("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings")
    public void processSubmissionSucceededWithWarningEvent(final JsonEnvelope caseOrApplicationFiltered) {
        processSubmissionSucceeded(caseOrApplicationFiltered);

    }

    private void processSubmissionSucceeded(final JsonEnvelope caseOrApplicationFiltered) {
        final String caseId = caseOrApplicationFiltered
                .payloadAsJsonObject()
                .getString("caseId", "");

        if (caseId.isEmpty()) {
            LOGGER.info("caseId not present so case not succeeded.");
        } else {
            // payload
            final RecordCaseStatus recordCaseStatus =
                    RecordCaseStatus
                            .recordCaseStatus()
                            .withStatus(CaseStatus.COMPLETED)
                            .withCaseId(UUID.fromString(caseId))
                            .build();
            // envelope
            final Envelope<RecordCaseStatus> envelope =
                    envelop(recordCaseStatus)
                            .withName(DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS)
                            .withMetadataFrom(caseOrApplicationFiltered);
            sender.send(envelope);
        }
    }

    @Handles("public.prosecutioncasefile.material-added")
    public void processMaterialAddedToCaseFile(final JsonEnvelope materialAddedToCaseJsonEnvelope) {
        final String documentSectionCode = materialAddedToCaseJsonEnvelope
                .payloadAsJsonObject()
                .getString("sectionCode", "");

        final String documentId = materialAddedToCaseJsonEnvelope
                .payloadAsJsonObject()
                .getJsonObject("material")
                .getString("fileStoreId", "");

        final Metadata metadata = materialAddedToCaseJsonEnvelope.metadata();
        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData = referenceDataServiceInterface.getDocumentTypeAccessBySectionCode(metadata, documentSectionCode);
        if (documentTypeAccessReferenceData != null
                && !documentTypeAccessReferenceData.getActionRequired()) {
            // payload
            final RemoveDocumentFromQueue removeDocumentFromQueue = removeDocumentFromQueue()
                    .withDocumentId(UUID.fromString(documentId))
                    .build();

            // envelope
            final Envelope<RemoveDocumentFromQueue> envelope =
                    envelop(removeDocumentFromQueue)
                            .withName(DOCUMENTQUEUE_COMMAND_REMOVE_DOCUMENT_FROM_QUEUE)
                            .withMetadataFrom(materialAddedToCaseJsonEnvelope);
            sender.send(envelope);
        } else {
            LOGGER.info("Document, {} is not requested to be removed from queue", documentId);
        }
    }

}
