package uk.gov.moj.cpp.prosecution.documentqueue.command.handler;


import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.QueueDocument;

import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class DocumentCaseHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("documentqueue.command.receive-outstanding-document")
    public void receiveOutstandingDocument(final Envelope<ReceiveOutstandingDocument> receiveOutstandingDocumentEnvelope) throws EventStreamException {

        final Document document = receiveOutstandingDocumentEnvelope.payload().getOutstandingDocument();
        final EventStream eventStream = eventSource.getStreamById(document.getScanDocumentId());
        final QueueDocument queueDocument = aggregateService.get(eventStream, QueueDocument.class);

        final Stream<Object> newEvents = queueDocument.receiveOutstandingDocument(document);
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(receiveOutstandingDocumentEnvelope)));
    }

    @Handles("documentqueue.command.update-document-status")
    public void updateDocumentStatus(final Envelope<UpdateDocumentStatus> updateDocumentStatusEnvelope) throws EventStreamException {

        final EventStream eventStream = eventSource.getStreamById(updateDocumentStatusEnvelope.payload().getDocumentId());
        final QueueDocument queueDocument = aggregateService.get(eventStream, QueueDocument.class);

        final Stream<Object> newEvents = queueDocument.updateDocumentStatus(updateDocumentStatusEnvelope.payload().getStatus());
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(updateDocumentStatusEnvelope)));
    }

}
