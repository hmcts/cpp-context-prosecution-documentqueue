package uk.gov.moj.cpp.prosecution.documentqueue.command.handler;


import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.documentqueue.command.handler.RecordCaseStatus;
import uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.CPPCase;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.QueueDocument;
import uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus;

import java.util.UUID;
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

    @Handles("documentqueue.command.link-document-to-case")
    public void linkOutstandingDocumentsToCase(final Envelope<LinkDocumentToCase> linkDocumentToCaseEnvelope) throws EventStreamException {

        final Document document = linkDocumentToCaseEnvelope.payload().getDocument();
        final EventStream eventStream = eventSource.getStreamById(document.getCaseId());
        final CPPCase cppCase = aggregateService.get(eventStream, CPPCase.class);

        final Stream<Object> newEvents = cppCase.linkDocumentToCase(document);
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(linkDocumentToCaseEnvelope)));
    }

    @Handles("documentqueue.command.update-document-status")
    public void updateDocumentStatus(final Envelope<UpdateDocumentStatus> updateDocumentStatusEnvelope) throws EventStreamException {

        final EventStream eventStream = eventSource.getStreamById(updateDocumentStatusEnvelope.payload().getDocumentId());
        final QueueDocument queueDocument = aggregateService.get(eventStream, QueueDocument.class);

        final Stream<Object> newEvents = queueDocument.updateDocumentStatus(updateDocumentStatusEnvelope.payload().getStatus(), false);
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(updateDocumentStatusEnvelope)));
    }

    @Handles("documentqueue.command.record-case-status")
    @SuppressWarnings("squid:S00112")
    public void recordCaseStatus(final Envelope<RecordCaseStatus> recordCaseStatusEnvelope) throws EventStreamException {

        final UUID caseId = recordCaseStatusEnvelope.payload().getCaseId();
        final CaseStatus caseStatus = recordCaseStatusEnvelope.payload().getStatus();
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CPPCase caseAggregate = aggregateService.get(eventStream, CPPCase.class);

        final Stream<Object> newEvents = caseAggregate.updateCaseStatus(caseStatus, caseId);
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(recordCaseStatusEnvelope)));
        caseAggregate.getDocuments().forEach(
                documentId -> {
                    final EventStream docEventStream = eventSource.getStreamById(documentId);
                    final QueueDocument queueDocumentAggregate = aggregateService.get(docEventStream, QueueDocument.class);
                    final Stream<Object> events = queueDocumentAggregate.updateDocumentStatus(Status.COMPLETED, true);
                    try {
                        docEventStream.append(events.map(toEnvelopeWithMetadataFrom(recordCaseStatusEnvelope)));
                    } catch (EventStreamException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Handles("documentqueue.command.attach-document")
    public void attachDocument(final Envelope<AttachDocument> attachDocumentEnvelope) throws  EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(attachDocumentEnvelope.payload().getDocumentId());
        final QueueDocument queueDocument = aggregateService.get(eventStream, QueueDocument.class);
        final Stream<Object> newEvents = queueDocument.receiveAttachDocument(attachDocumentEnvelope.payload().getCourtDocument());
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(attachDocumentEnvelope)));
    }

    @Handles("documentqueue.command.record-document-attached")
    public void recordDocumentAttached(final Envelope<RecordDocumentAttached> recordDocumentAttachedEnvelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(recordDocumentAttachedEnvelope.payload().getDocumentId());
        final QueueDocument queueDocument = aggregateService.get(eventStream, QueueDocument.class);
        final Stream<Object> newEvents = queueDocument.attachDocument(recordDocumentAttachedEnvelope.payload().getDocumentId());
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(recordDocumentAttachedEnvelope)));
    }

}
