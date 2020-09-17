package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument.scanDocument;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.documentqueue.event.DocumentLinkedToCase.documentLinkedToCase;

import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.documentqueue.event.DocumentLinkedToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.DocumentQueryView;

import java.util.UUID;

import javax.json.JsonObject;
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

    @Captor
    private ArgumentCaptor<Envelope> argumentCaptor;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private DocumentQueryView documentQueryView;

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
    public void shouldMarkDocumentCompleted() {

        final String documentId = randomUUID().toString();
        final JsonEnvelope eventEnvelope = createEnvelope("documentqueue.event.document-marked-completed",
                createObjectBuilder()
                        .add("documentId", documentId)
                        .build());

        final UUID envelopeId = randomUUID();

        when(documentQueryView.getDocument(any())).thenReturn(envelopeFrom(metadataFrom(eventEnvelope.metadata()), scanDocument().withEnvelopeId(envelopeId).build()));
        documentqueueEventProcessor.processDocumentMarkedCompleted(eventEnvelope);

        verify(documentQueryView, times(1)).getDocument(any());
        verify(sender, times(1)).sendAsAdmin(argumentCaptor.capture());
        verifyNoMoreInteractions(sender);

        final Envelope<JsonObject> jsonObjectEnvelope = argumentCaptor.getValue();

        assertThat(jsonObjectEnvelope.payload().getString("scanDocumentId"), is(documentId));
        assertThat(jsonObjectEnvelope.payload().getString("scanEnvelopeId"), is(envelopeId.toString()));
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

    @Test
    public void shouldProcessDocumentLinkedToCase() {
        // given
        final UUID prosecutionCaseId = randomUUID();
        final Document document1 = mock(Document.class);

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("documentqueue.event.document-linked-to-case")
                .build();
        final Envelope<DocumentLinkedToCase> envelope = envelopeFrom(metadata, documentLinkedToCase().withDocument(document1).build());

        // when the ejected event is processed
        documentqueueEventProcessor.processDocumentLinkedToCase(envelope);

        // then fire a command record the case status
        verify(sender).send(argumentCaptor.capture());
        final Envelope<ReceiveOutstandingDocument> receiveOutstandingDocumentEnvelope = argumentCaptor.getValue();

        final ReceiveOutstandingDocument receiveOutstandingDocument = receiveOutstandingDocumentEnvelope.payload();
        final Metadata metadataOfSendRequest = receiveOutstandingDocumentEnvelope.metadata();
        assertThat(receiveOutstandingDocument.getOutstandingDocument(), is(document1));
        assertThat(metadataOfSendRequest.name(), is("documentqueue.command.receive-outstanding-document"));
    }

}