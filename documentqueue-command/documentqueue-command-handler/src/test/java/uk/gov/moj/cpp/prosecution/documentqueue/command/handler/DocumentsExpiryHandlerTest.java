package uk.gov.moj.cpp.prosecution.documentqueue.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.DeleteExpiredDocuments.deleteExpiredDocuments;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDeleteExpiredDocumentsAsRequested.markDeleteExpiredDocumentsAsRequested;
import static uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.DocumentsExpiration.STREAM_ID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequestReceived;
import uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequested;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.DocumentsExpiration;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentsExpiryHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private DocumentsExpiryHandler documentsExpiryHandler;

    @BeforeEach
    public void setup() throws Exception {
        createEnveloperWithEvents(
                DeleteExpiredDocumentsRequestReceived.class,
                DeleteExpiredDocumentsRequested.class);
    }

    @Test
    public void shouldHandleDeleteExpiredDocuments() throws Exception {
        final DeleteExpiredDocuments deleteExpiredDocuments =
                deleteExpiredDocuments()
                        .build();
        final EventStream eventStream = mock(EventStream.class);
        final DocumentsExpiration documentsExpiration = new DocumentsExpiration();

        when(eventSource.getStreamById(STREAM_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DocumentsExpiration.class)).thenReturn(documentsExpiration);

        final UUID commandId = randomUUID();
        final Envelope<DeleteExpiredDocuments> deleteExpiredDocumentsEnvelope =
                envelopeFrom(metadataBuilder()
                                .withId(commandId)
                                .withName("documentqueue.command.delete-expired-documents"),
                        deleteExpiredDocuments);

        documentsExpiryHandler.handleDeleteExpiredDocuments(deleteExpiredDocumentsEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.delete-expired-documents-request-received"),
                        payloadIsJson(allOf(
                                withJsonPath("$.requestedAt", notNullValue())
                        )))
                        .thatMatchesSchema()
                )
        );
    }

    @Test
    public void shouldHandleMarkDeleteExpiredDocumentsAsRequested() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final List<UUID> documentIds = Arrays.asList(documentId);
        final MarkDeleteExpiredDocumentsAsRequested markDeleteExpiredDocumentsAsRequested =
                markDeleteExpiredDocumentsAsRequested()
                        .withDocumentIds(documentIds)
                        .build();
        final EventStream eventStream = mock(EventStream.class);
        final DocumentsExpiration documentsExpiration = new DocumentsExpiration();

        when(eventSource.getStreamById(STREAM_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DocumentsExpiration.class)).thenReturn(documentsExpiration);

        final UUID commandId = randomUUID();
        final Envelope<MarkDeleteExpiredDocumentsAsRequested> markDeleteExpiredDocumentsAsRequestedEnvelope =
                envelopeFrom(metadataBuilder()
                                .withId(commandId)
                                .withName("documentqueue.command.mark-delete-expired-documents-as-requested"),
                        markDeleteExpiredDocumentsAsRequested);

        documentsExpiryHandler.handleMarkDeleteExpiredDocumentsAsRequested(markDeleteExpiredDocumentsAsRequestedEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.delete-expired-documents-requested"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentIds[0]", is(documentId.toString()))
                        )))
                        .thatMatchesSchema()
                )
        );
    }

}