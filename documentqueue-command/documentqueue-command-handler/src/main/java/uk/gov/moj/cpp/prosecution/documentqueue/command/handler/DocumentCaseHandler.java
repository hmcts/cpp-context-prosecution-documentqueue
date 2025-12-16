package uk.gov.moj.cpp.prosecution.documentqueue.command.handler;


import static uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentDeleteStatus.documentDeleteStatus;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.DeleteDocuments.DELETE_DOCUMENTS_STEAM_ID;

import uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentDeleteStatus;
import uk.gov.justice.cpp.prosecution.documentqueue.domain.ProsecutionCaseDocStatus;
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
import uk.gov.moj.cpp.documentqueue.command.handler.RemoveDocumentFromQueue;
import uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.CPPCase;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.DeleteDocuments;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.QueueDocument;
import uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus;

import java.util.ArrayList;
import java.util.List;
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

    @Handles("documentqueue.command.remove-document-from-queue")
    public void removeDocumentFromQueue(final Envelope<RemoveDocumentFromQueue> removeDocumentFromQueueEnvelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(removeDocumentFromQueueEnvelope.payload().getDocumentId());

        final QueueDocument queueDocument = aggregateService.get(eventStream, QueueDocument.class);
        final Stream<Object> newEvents = queueDocument.removeDocumentFromQueue();

        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(removeDocumentFromQueueEnvelope)));
    }

    @Handles("documentqueue.command.record-case-status")
    @SuppressWarnings("squid:S00112")
    public void recordCaseStatus(final Envelope<RecordCaseStatus> recordCaseStatusEnvelope) throws EventStreamException {

        final UUID caseId = recordCaseStatusEnvelope.payload().getCaseId();
        final CaseStatus caseStatus = recordCaseStatusEnvelope.payload().getStatus();
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CPPCase caseAggregate = aggregateService.get(eventStream, CPPCase.class);

        final Stream<Object> newEvents = caseAggregate.updateCaseStatus(caseStatus, caseId);
        if (CaseStatus.COMPLETED.equals(caseStatus)) {
            eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(recordCaseStatusEnvelope)));
            return;
        }
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(recordCaseStatusEnvelope)));
        caseAggregate.getDocuments().forEach(
                documentId -> {
                    final EventStream docEventStream = eventSource.getStreamById(documentId);
                    final QueueDocument queueDocumentAggregate = aggregateService.get(docEventStream, QueueDocument.class);
                    Stream<Object> events = Stream.empty();
                    if (caseAggregate.getCaseStatus() != null) {
                        if (CaseStatus.EJECTED.equals(caseStatus)) {
                            events = queueDocumentAggregate.updateDocumentStatus(Status.COMPLETED, true);
                        } else if (CaseStatus.FILTERED.equals(caseStatus)) {
                            events = Stream.concat(queueDocumentAggregate.updateDocumentStatus(Status.DELETED, true),
                                    queueDocumentAggregate.requestDocumentDeleteFromFileStore());
                        }
                        try {
                            docEventStream.append(events.map(toEnvelopeWithMetadataFrom(recordCaseStatusEnvelope)));
                        } catch (EventStreamException e) {
                            throw new RuntimeException(e);
                        }
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

    @Handles("documentqueue.command.delete-documents-of-cases")
    public void deleteDocumentsOfCases(final Envelope<DeleteDocumentsOfCases> deleteDocumentsOfCasesEnvelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(DELETE_DOCUMENTS_STEAM_ID);
        final DeleteDocuments deleteDocumentsOfCases = aggregateService.get(eventStream, DeleteDocuments.class);
        final Stream<Object> newEvents = deleteDocumentsOfCases
                .saveRequestReceived(deleteDocumentsOfCasesEnvelope.payload().getCaseUrns(), deleteDocumentsOfCasesEnvelope.payload().getCasePTIUrns());
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(deleteDocumentsOfCasesEnvelope)));
    }

    @Handles("documentqueue.command.mark-documents-deleted-for-cases")
    public void markDocumentsDeletedForCases(final Envelope<MarkDocumentsDeletedForCases> markDocumentsDeletedForCasesEnvelope) throws EventStreamException {
        final List<ProsecutionCaseDocStatus> deletedDocumentsCases = new ArrayList<>();
        markDocumentsDeletedForCasesEnvelope
                .payload()
                .getProsecutionCases()
                .forEach(prosecutionCase -> handleProsecutionCase(markDocumentsDeletedForCasesEnvelope, deletedDocumentsCases, prosecutionCase));

        final EventStream eventStream = eventSource.getStreamById(DELETE_DOCUMENTS_STEAM_ID);
        final DeleteDocuments deleteDocumentsOfCases = aggregateService.get(eventStream, DeleteDocuments.class);
        final Stream<Object> newEvents = deleteDocumentsOfCases.deleteDocumentsOfCases(deletedDocumentsCases);
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(markDocumentsDeletedForCasesEnvelope)));
    }

    @Handles("documentqueue.command.mark-document-deleted-from-file-store")
    public void setMarkDocumentDeletedFromFileStore(final Envelope<MarkDocumentDeletedFromFilestore> markDocumentDeletedFromFileStoreEnvelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(markDocumentDeletedFromFileStoreEnvelope.payload().getDocumentId());
        final QueueDocument queueDocumentAggregate = aggregateService.get(eventStream, QueueDocument.class);
        final Stream<Object> newEvents = queueDocumentAggregate.markDocumentDeletedFromFileStore();
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(markDocumentDeletedFromFileStoreEnvelope)));
    }

    private void handleProsecutionCase(final Envelope<MarkDocumentsDeletedForCases> markDocumentsDeletedForCasesEnvelope,
                                       final List<ProsecutionCaseDocStatus> deletedDocumentsCases,
                                       final uk.gov.justice.cpp.prosecution.documentqueue.domain.ProsecutionCase prosecutionCase) {
        final List<DocumentDeleteStatus> documentDeleteStatusList = new ArrayList<>();
        final ProsecutionCaseDocStatus.Builder prosecutionCaseDocStatusBuilder = ProsecutionCaseDocStatus.prosecutionCaseDocStatus();
        prosecutionCaseDocStatusBuilder.withCaseId(prosecutionCase.getCaseId())
                .withCaseUrn(prosecutionCase.getCaseUrn())
                .withCasePTIUrn(prosecutionCase.getCasePTIUrn())
                .withCaseId(prosecutionCase.getCaseId())
                .withDocumentDeleteStatusList(documentDeleteStatusList);
        deletedDocumentsCases.add(prosecutionCaseDocStatusBuilder.build());

        prosecutionCase.getDocumentIds()
                .forEach(documentId -> handleDocument(markDocumentsDeletedForCasesEnvelope, documentDeleteStatusList, documentId));
    }

    @SuppressWarnings("squid:S00112")
    private void handleDocument(final Envelope<MarkDocumentsDeletedForCases> markDocumentsDeletedForCasesEnvelope,
                                final List<DocumentDeleteStatus> documentDeleteStatusList,
                                final UUID documentId) {
        final EventStream docEventStream = eventSource.getStreamById(documentId);
        final QueueDocument queueDocumentAggregate = aggregateService.get(docEventStream, QueueDocument.class);
        Stream<Object>  eventStream = Stream.empty();
        if (queueDocumentAggregate.getFileServiceId() != null) {
            documentDeleteStatusList.add(
                    documentDeleteStatus()
                            .withDeleteStatus("Yes")
                            .withDocumentId(documentId)
                            .build());
            eventStream = Stream.concat(
                    queueDocumentAggregate.updateDocumentStatus(Status.DELETED, true),
                    queueDocumentAggregate.requestDocumentDeleteFromFileStore());
        } else {
            documentDeleteStatusList.add(
                    documentDeleteStatus()
                            .withDocumentId(documentId)
                            .withDeleteStatus("No")
                            .build());
        }
        try {
            docEventStream.append(eventStream.map(toEnvelopeWithMetadataFrom(markDocumentsDeletedForCasesEnvelope)));
        } catch (EventStreamException e) {
            throw new RuntimeException(e);
        }
    }


}
