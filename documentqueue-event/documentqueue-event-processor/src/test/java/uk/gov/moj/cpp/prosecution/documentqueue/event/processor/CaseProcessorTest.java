package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.prosecution.documentqueue.event.processor.CaseProcessor.DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.documentqueue.command.handler.RecordCaseStatus;
import uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<RecordCaseStatus>> argumentCaptor;

    @InjectMocks
    private CaseProcessor caseProcessor;

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
        verify(sender).send(argumentCaptor.capture());
        final Envelope<RecordCaseStatus> recordCaseStatusEnvelope = argumentCaptor.getValue();

        final RecordCaseStatus recordCaseStatus = recordCaseStatusEnvelope.payload();
        final Metadata metadataOfSendRequest = recordCaseStatusEnvelope.metadata();
        assertThat(recordCaseStatus.getCaseId(), is(prosecutionCaseId));
        assertThat(recordCaseStatus.getStatus(), is(CaseStatus.EJECTED));
        assertThat(metadataOfSendRequest.name(), is(DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS));
    }

}