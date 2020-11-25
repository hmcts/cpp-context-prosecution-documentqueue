package uk.gov.moj.cpp.prosecution.documentqueue.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.DocumentsExpiration;

import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class DocumentsExpiryHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("documentqueue.command.delete-expired-documents")
    public void handleDeleteExpiredDocuments(final Envelope<DeleteExpiredDocuments> deleteExpiredDocumentsEnvelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(DocumentsExpiration.STREAM_ID);
        final DocumentsExpiration documentsExpiration = aggregateService.get(eventStream, DocumentsExpiration.class);

        final Stream<Object> newEvents = documentsExpiration.deleteExpiredDocuments();
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(deleteExpiredDocumentsEnvelope)));
    }

    @Handles("documentqueue.command.mark-delete-expired-documents-as-requested")
    public void handleMarkDeleteExpiredDocumentsAsRequested(final Envelope<MarkDeleteExpiredDocumentsAsRequested> markDeleteExpiredDocumentsAsRequestedEnvelope) throws EventStreamException {
        final MarkDeleteExpiredDocumentsAsRequested payload = markDeleteExpiredDocumentsAsRequestedEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(DocumentsExpiration.STREAM_ID);
        final DocumentsExpiration documentsExpiration = aggregateService.get(eventStream, DocumentsExpiration.class);

        final Stream<Object> newEvents = documentsExpiration.markDeleteExpiredDocumentsAsRequested(payload.getDocumentIds());
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(markDeleteExpiredDocumentsAsRequestedEnvelope)));
    }

}
