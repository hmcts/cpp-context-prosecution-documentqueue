package uk.gov.moj.cpp.prosecution.documentqueue.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentQueueApiTest {

    private static final String DELETE_DOCUMENTS_OF_CASES = "documentqueue.delete-documents-of-cases";
    private static final String DELETE_EXPIRED_DOCUMENTS = "documentqueue.delete-expired-documents";
    private static final String DELETE_DOCUMENTS_OF_CASES_COMMAND = "documentqueue.command.delete-documents-of-cases";
    private static final String DELETE_EXPIRED_DOCUMENTS_COMMAND = "documentqueue.command.delete-expired-documents";
    private static final String ATTACH_DOCUMENT_NAME = "documentqueue.attach-document";
    private static final String ATTACH_DOCUMENT_COMMAND_NAME = "documentqueue.command.attach-document";

    @Mock
    private Sender sender;

    @InjectMocks
    private DocumentQueueApi documentQueueApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleDocumentQuery() {

        final String documentId = randomUUID().toString();
        final JsonEnvelope jsonEnvelope = createEnvelope("documentqueue.event.document-status-updated",
                createObjectBuilder()
                        .add("documentId", documentId)
                        .add("status", IN_PROGRESS.toString())
                        .build());

        documentQueueApi.updateDocumentStatus(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonValue> jsonValueEnvelope = envelopeCaptor.getValue();

        assertThat(jsonValueEnvelope.metadata(), withMetadataEnvelopedFrom(jsonEnvelope).withName("documentqueue.command.update-document-status"));
        assertThat(jsonValueEnvelope.payload(), payloadIsJson(allOf(
                withJsonPath("$.documentId", equalTo(documentId)),
                withJsonPath("$.status", equalTo(IN_PROGRESS.toString()))
        )));
    }

    @Test
    public void shouldHandleAttachDocumentCommand() {
        assertThat(DocumentQueueApi.class, isHandlerClass(COMMAND_API)
                .with(method("attachDocument").thatHandles(ATTACH_DOCUMENT_NAME)));
    }

    @Test
    public void shouldAttachDocument() {

        final JsonEnvelope commandEnvelope = buildAttachDocumentEnvelope();

        documentQueueApi.attachDocument(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ATTACH_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payload()));
    }

    @Test
    public void shouldDeleteDocumentsOfCases() {
        final JsonEnvelope commandEnvelope = buildCommandEnvelopeForDeleteDocs();

        documentQueueApi.deleteDocumentsOfCases(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(DELETE_DOCUMENTS_OF_CASES_COMMAND));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payload()));
    }

    @Test
    public void shouldDeleteExpiredDocuments() {
        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider()
                .envelopeFrom(metadataBuilder()
                        .withName(DELETE_EXPIRED_DOCUMENTS)
                        .withId(randomUUID())
                        .withUserId(randomUUID().toString())
                        .build(), null);

        documentQueueApi.deleteExpiredDocuments(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(DELETE_EXPIRED_DOCUMENTS_COMMAND));
        assertThat(newCommand.payload(), is(JsonValue.NULL));
    }

    private JsonEnvelope buildCommandEnvelopeForDeleteDocs() {
        final JsonObject payload = createObjectBuilder()
                .add("casePTIUrns",
                        JsonObjects.getProvider().createArrayBuilder()
                                .add("URN1")
                                .build())
                .build();

        final Metadata metadata = metadataBuilder()
                .withName(DELETE_DOCUMENTS_OF_CASES)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope =  new DefaultJsonEnvelopeProvider()
                .envelopeFrom(metadata, payload);
        return commandEnvelope;
    }

    private JsonEnvelope buildAttachDocumentEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("courtDocument", createObjectBuilder()
                        .add("courtDocumentId", randomUUID().toString())
                        .add("documentCategory", createObjectBuilder().build())
                        .add("name", "Document Name")
                        .add("documentTypeId", randomUUID().toString())
                        .add("materials", uk.gov.justice.services.messaging.JsonObjects.getProvider().createArrayBuilder().build())
                        .add("containsFinancialMeans", true)
                        .build())
                .build();

        final Metadata metadata = metadataBuilder()
                .withName(ATTACH_DOCUMENT_NAME)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}
