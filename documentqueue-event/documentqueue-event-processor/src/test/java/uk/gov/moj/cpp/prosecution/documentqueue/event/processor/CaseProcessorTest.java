package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.documentqueue.event.processor.CaseProcessor.DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS;
import static uk.gov.moj.cpp.prosecution.documentqueue.event.processor.CaseProcessor.DOCUMENTQUEUE_COMMAND_REMOVE_DOCUMENT_FROM_QUEUE;

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
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private ReferenceDataServiceInterface referenceDataServiceInterface;

    @InjectMocks
    private CaseProcessor caseProcessor;

    @Captor
    private ArgumentCaptor<Envelope<RecordCaseStatus>> argumentCaptorRecordCaseStatus;

    @Captor
    private ArgumentCaptor<Envelope<RemoveDocumentFromQueue>> argumentCaptorRemoveDocumentFromQueue;

    @Test
    public void shouldSendACommandToRecordCaseStatus() {
        // given
        final UUID userId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("public.progression.events.case-or-application-ejected")
                .withUserId(userId.toString())
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("prosecutionCaseId", prosecutionCaseId.toString())
                .build();
        final JsonEnvelope caseOrApplicationEjected = JsonEnvelope
                .envelopeFrom(metadata, payload);

        // when the ejected event is processed
        caseProcessor.processCaseRejectedPublicEvent(caseOrApplicationEjected);

        // then fire a command record the case status
        verify(sender).send(argumentCaptorRecordCaseStatus.capture());
        final Envelope<RecordCaseStatus> recordCaseStatusEnvelope = argumentCaptorRecordCaseStatus.getValue();

        final RecordCaseStatus recordCaseStatus = recordCaseStatusEnvelope.payload();
        final Metadata metadataOfSendRequest = recordCaseStatusEnvelope.metadata();
        assertThat(recordCaseStatus.getCaseId(), is(prosecutionCaseId));
        assertThat(recordCaseStatus.getStatus(), is(CaseStatus.EJECTED));
        assertThat(metadataOfSendRequest.name(), is(DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS));
    }

    @Test
    public void shouldSendACommandToRecordCaseStatusAsCompleted() {
        // given
        final UUID userId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("public.prosecutioncasefile.prosecution-submission-succeeded")
                .withUserId(userId.toString())
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("caseId", prosecutionCaseId.toString())
                .build();
        final JsonEnvelope caseOrApplicationSubmissionSucceeded = JsonEnvelope
                .envelopeFrom(metadata, payload);

        // when the ejected event is processed
        caseProcessor.processSubmissionSucceededEvent(caseOrApplicationSubmissionSucceeded);

        // then fire a command record the case status
        verify(sender).send(argumentCaptorRecordCaseStatus.capture());
        final Envelope<RecordCaseStatus> recordCaseStatusEnvelope = argumentCaptorRecordCaseStatus.getValue();

        final RecordCaseStatus recordCaseStatus = recordCaseStatusEnvelope.payload();
        final Metadata metadataOfSendRequest = recordCaseStatusEnvelope.metadata();
        assertThat(recordCaseStatus.getCaseId(), is(prosecutionCaseId));
        assertThat(recordCaseStatus.getStatus(), is(CaseStatus.COMPLETED));
        assertThat(metadataOfSendRequest.name(), is(DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS));
    }

    @Test
    public void shouldSendACommandToRecordCaseStatusAsCompletedWithWarning() {
        // given
        final UUID userId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings")
                .withUserId(userId.toString())
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("caseId", prosecutionCaseId.toString())
                .build();
        final JsonEnvelope caseOrApplicationSubmissionSucceeded = JsonEnvelope
                .envelopeFrom(metadata, payload);

        // when the ejected event is processed
        caseProcessor.processSubmissionSucceededEvent(caseOrApplicationSubmissionSucceeded);

        // then fire a command record the case status
        verify(sender).send(argumentCaptorRecordCaseStatus.capture());
        final Envelope<RecordCaseStatus> recordCaseStatusEnvelope = argumentCaptorRecordCaseStatus.getValue();

        final RecordCaseStatus recordCaseStatus = recordCaseStatusEnvelope.payload();
        final Metadata metadataOfSendRequest = recordCaseStatusEnvelope.metadata();
        assertThat(recordCaseStatus.getCaseId(), is(prosecutionCaseId));
        assertThat(recordCaseStatus.getStatus(), is(CaseStatus.COMPLETED));
        assertThat(metadataOfSendRequest.name(), is(DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS));
    }
    @Test
    public void shouldSendACommandToRemoveDocumentFromQueueWhenActionIsNotRequiredOnDocumentType() {
        // given
        final UUID userId = randomUUID();
        final UUID documentId = randomUUID();

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("public.prosecutioncasefile.material-added")
                .withUserId(userId.toString())
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("material",
                        createObjectBuilder()
                                .add("fileStoreId", documentId.toString()))
                .build();
        final JsonEnvelope caseOrApplicationEjected = JsonEnvelope
                .envelopeFrom(metadata, payload);

        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData =
                DocumentTypeAccessReferenceData
                        .documentTypeAccessReferenceData()
                        .withActionRequired(false)
                        .build();

        when(referenceDataServiceInterface.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(documentTypeAccessReferenceData);

        // when the ejected event is processed
        caseProcessor.processMaterialAddedToCaseFile(caseOrApplicationEjected);

        // then fire a command record the case status
        verify(sender).send(argumentCaptorRemoveDocumentFromQueue.capture());
        final Envelope<RemoveDocumentFromQueue> removeDocumentFromQueueEnvelope = argumentCaptorRemoveDocumentFromQueue.getValue();

        final RemoveDocumentFromQueue removeDocumentFromQueue = removeDocumentFromQueueEnvelope.payload();
        final Metadata metadataOfSendRequest = removeDocumentFromQueueEnvelope.metadata();
        assertThat(removeDocumentFromQueue.getDocumentId(), is(documentId));
        assertThat(metadataOfSendRequest.name(), is(DOCUMENTQUEUE_COMMAND_REMOVE_DOCUMENT_FROM_QUEUE));
    }

    @Test
    public void shouldNotSendACommandToRemoveDocumentFromQueueWhenActionIsRequiredOnDocumentType() {
        // given
        final UUID userId = randomUUID();
        final UUID documentId = randomUUID();

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("public.prosecutioncasefile.material-added")
                .withUserId(userId.toString())
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("material",
                        createObjectBuilder()
                                .add("fileStoreId", documentId.toString()))
                .build();
        final JsonEnvelope caseOrApplicationEjected = JsonEnvelope
                .envelopeFrom(metadata, payload);

        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData =
                DocumentTypeAccessReferenceData
                        .documentTypeAccessReferenceData()
                        .withActionRequired(true)
                        .build();
        when(referenceDataServiceInterface.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(documentTypeAccessReferenceData);

        // when the ejected event is processed
        caseProcessor.processMaterialAddedToCaseFile(caseOrApplicationEjected);

        // then fire a command record the case status
        verify(sender, times(0)).send(argumentCaptorRecordCaseStatus.capture());
    }
}