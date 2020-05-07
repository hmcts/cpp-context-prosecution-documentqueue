package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class DocumentqueueEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private Logger logger;

    @InjectMocks
    private DocumentqueueEventProcessor documentqueueEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldSendPublicDocumentStatusUpdated() {

        final String documentId = randomUUID().toString();
        final JsonEnvelope eventEnvelope = createEnvelope("documentqueue.event.document-status-updated",
                createObjectBuilder()
                        .add("documentId", documentId)
                        .add("status", IN_PROGRESS.toString())
                        .build());

        when(logger.isDebugEnabled()).thenReturn(false);

        documentqueueEventProcessor.processDocumentStatusUpdated(eventEnvelope);

        verify(logger).isDebugEnabled();
        verifyNoMoreInteractions(logger);
        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonValue> jsonValueEnvelope = envelopeCaptor.getValue();

        assertThat(jsonValueEnvelope.metadata(), withMetadataEnvelopedFrom(eventEnvelope).withName("public.documentqueue.document-status-updated"));
        assertThat(jsonValueEnvelope.payload(), payloadIsJson(allOf(
                withJsonPath("$.documentId", equalTo(documentId)),
                withJsonPath("$.status", equalTo(IN_PROGRESS.toString()))
        )));
    }

    @Test
    public void shouldLogDebugMessage() {

        final String documentId = randomUUID().toString();

        final JsonEnvelope eventEnvelope = createEnvelope("documentqueue.event.document-status-updated",
                createObjectBuilder()
                        .add("documentId", documentId)
                        .add("status", IN_PROGRESS.toString())
                        .build());

        when(logger.isDebugEnabled()).thenReturn(true);

        documentqueueEventProcessor.processDocumentStatusUpdated(eventEnvelope);

        verify(logger).debug("public.documentqueue.document-status-updated {}", eventEnvelope.toObfuscatedDebugString());
    }
}