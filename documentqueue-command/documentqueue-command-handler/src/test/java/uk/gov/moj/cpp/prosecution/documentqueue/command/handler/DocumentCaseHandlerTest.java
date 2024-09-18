package uk.gov.moj.cpp.prosecution.documentqueue.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.prosecution.documentqueue.domain.Document.document;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.COMPLETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.DELETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamWithEmptyStream;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.documentqueue.command.handler.RecordCaseStatus.recordCaseStatus;
import static uk.gov.moj.cpp.documentqueue.command.handler.RemoveDocumentFromQueue.removeDocumentFromQueue;
import static uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus.updateDocumentStatus;
import static uk.gov.moj.cpp.documentqueue.event.DocumentMarkedCompleted.documentMarkedCompleted;
import static uk.gov.moj.cpp.documentqueue.event.DocumentMarkedInprogress.documentMarkedInprogress;
import static uk.gov.moj.cpp.documentqueue.event.OutstandingDocumentReceived.outstandingDocumentReceived;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument.receiveOutstandingDocument;
import static uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.DeleteDocuments.DELETE_DOCUMENTS_STEAM_ID;

import uk.gov.justice.cpp.prosecution.documentqueue.domain.ProsecutionCase;
import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.model.CourtDocument;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.DocumentStatusUpdated;
import uk.gov.moj.cpp.documentqueue.command.handler.RecordCaseStatus;
import uk.gov.moj.cpp.documentqueue.command.handler.RemoveDocumentFromQueue;
import uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus;
import uk.gov.moj.cpp.documentqueue.event.AttachDocumentRequested;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedEjected;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedFiltered;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedSubmissionSucceeded;
import uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesActioned;
import uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentDeleteFromFileStoreRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentDeletedFromFileStore;
import uk.gov.moj.cpp.documentqueue.event.DocumentLinkedToCase;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedCompleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedDeleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedInprogress;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedOutstanding;
import uk.gov.moj.cpp.documentqueue.event.DocumentNotLinkedToCase;
import uk.gov.moj.cpp.documentqueue.event.DocumentStatusUpdateFailed;
import uk.gov.moj.cpp.documentqueue.event.OutstandingDocumentReceived;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.CPPCase;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.DeleteDocuments;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.QueueDocument;
import uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentCaseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private DocumentCaseHandler documentCaseHandler;

    @BeforeEach
    public void setup() throws Exception {
        createEnveloperWithEvents(
                OutstandingDocumentReceived.class,
                DocumentMarkedOutstanding.class,
                DocumentMarkedInprogress.class,
                DocumentMarkedCompleted.class,
                DocumentMarkedDeleted.class,
                DocumentStatusUpdated.class,
                DocumentStatusUpdateFailed.class,
                AttachDocumentRequested.class,
                DocumentLinkedToCase.class,
                DocumentNotLinkedToCase.class,
                CaseMarkedEjected.class,
                CaseMarkedFiltered.class,
                CaseMarkedSubmissionSucceeded.class,
                DeleteDocumentsOfCasesRequested.class,
                DeleteDocumentsOfCasesActioned.class,
                DocumentDeleteFromFileStoreRequested.class,
                DocumentDeletedFromFileStore.class);
    }

    @Test
    public void shouldReceiveOutstandingDocument() throws Exception {

        final UUID scanDocumentId = randomUUID();
        final String fileName = "theFile.pdf";
        final EventStream eventStream = mock(EventStream.class);
        final QueueDocument queueDocument = new QueueDocument();

        final ReceiveOutstandingDocument receiveOutstandingDocument = receiveOutstandingDocument()
                .withOutstandingDocument(document()
                        .withScanDocumentId(scanDocumentId)
                        .withStatus(OUTSTANDING)
                        .withFileName(fileName)
                        .build())
                .build();

        final UUID commandId = randomUUID();
        final Envelope<ReceiveOutstandingDocument> receiveOutstandingDocumentEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.receive-outstanding-document"),
                receiveOutstandingDocument);

        when(eventSource.getStreamById(scanDocumentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);

        documentCaseHandler.receiveOutstandingDocument(receiveOutstandingDocumentEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.outstanding-document-received"),
                        payloadIsJson(allOf(
                                withJsonPath("$.outstandingDocument.scanDocumentId", equalTo(scanDocumentId.toString())),
                                withJsonPath("$.outstandingDocument.status", equalTo(OUTSTANDING.toString())),
                                withJsonPath("$.outstandingDocument.fileName", equalTo(fileName))
                        )))
                        .thatMatchesSchema()
                )
        );
    }

    @Test
    public void shouldraiseDocumentAttachRequestedEvent() throws Exception {
        final UUID documentId = randomUUID();
        final UUID commandId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final AttachDocument attachDocumentRequested = AttachDocument.attachDocument()
                .withDocumentId(documentId)
                .withCourtDocument(CourtDocument.courtDocument().withCourtDocumentId(courtDocumentId).build()).build();


        final OutstandingDocumentReceived outstandingDocumentReceived = outstandingDocumentReceived()
                .withOutstandingDocument(document()
                        .withScanDocumentId(documentId)
                        .withSource(Source.CPS)
                        .withStatus(OUTSTANDING)
                        .build())
                .build();

        final QueueDocument queueDocument = new QueueDocument();
        queueDocument.apply(outstandingDocumentReceived);
        queueDocument.apply(new DocumentMarkedInprogress(documentId));

        final EventStream eventStream = mock(EventStream.class);
        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);
        final Envelope<AttachDocument> attachDocumentRequestedEnvelope = envelopeFrom(metadataBuilder().withId(commandId).withName("documentqueue.command.attach-document"), attachDocumentRequested);

        documentCaseHandler.attachDocument(attachDocumentRequestedEnvelope);
        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.attach-document-requested"),
                        payloadIsJson(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        ))
                )
        );


    }

    @Test
    public void shouldUpdateDocumentStatusToInProgress() throws Exception {

        final UUID commandId = randomUUID();
        final UUID documentId = randomUUID();
        final EventStream eventStream = mock(EventStream.class);

        final OutstandingDocumentReceived outstandingDocumentReceived = outstandingDocumentReceived()
                .withOutstandingDocument(document()
                        .withScanDocumentId(documentId)
                        .withStatus(OUTSTANDING)
                        .build())
                .build();

        final QueueDocument queueDocument = new QueueDocument();

        queueDocument.apply(outstandingDocumentReceived);

        final UpdateDocumentStatus updateDocumentStatus = updateDocumentStatus().withDocumentId(documentId).withStatus(IN_PROGRESS).build();

        final Envelope<UpdateDocumentStatus> updateDocumentStatusEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.update-document-status"),
                updateDocumentStatus);

        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);

        documentCaseHandler.updateDocumentStatus(updateDocumentStatusEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-inprogress"),
                        payloadIsJson(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        ))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-status-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString())),
                                withJsonPath("$.status", equalTo(IN_PROGRESS.toString()))
                        )))
                        .thatMatchesSchema()
                )
        );
    }

    @Test
    public void shouldUpdateDocumentStatusToCompleted() throws Exception {

        final UUID commandId = randomUUID();
        final UUID documentId = randomUUID();
        final EventStream eventStream = mock(EventStream.class);

        final OutstandingDocumentReceived outstandingDocumentReceived = outstandingDocumentReceived()
                .withOutstandingDocument(document()
                        .withScanDocumentId(documentId)
                        .withStatus(OUTSTANDING)
                        .build())
                .build();

        final QueueDocument queueDocument = new QueueDocument();
        queueDocument.apply(Stream.of(
                outstandingDocumentReceived,
                documentMarkedInprogress().withDocumentId(documentId).build()));

        final UpdateDocumentStatus updateDocumentStatus = updateDocumentStatus().withDocumentId(documentId).withStatus(COMPLETED).build();

        final Envelope<UpdateDocumentStatus> updateDocumentStatusEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.update-document-status"),
                updateDocumentStatus);

        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);

        documentCaseHandler.updateDocumentStatus(updateDocumentStatusEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-completed"),
                        payloadIsJson(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        ))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-status-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString())),
                                withJsonPath("$.status", equalTo(COMPLETED.toString()))
                        )))
                        .thatMatchesSchema()
                )
        );
    }

    @Test
    public void shouldUpdateDocumentStatusToOutstanding() throws Exception {

        final UUID commandId = randomUUID();
        final UUID documentId = randomUUID();
        final EventStream eventStream = mock(EventStream.class);

        final OutstandingDocumentReceived outstandingDocumentReceived = outstandingDocumentReceived()
                .withOutstandingDocument(document()
                        .withScanDocumentId(documentId)
                        .withStatus(OUTSTANDING)
                        .build())
                .build();

        final QueueDocument queueDocument = new QueueDocument();
        queueDocument.apply(Stream.of(
                outstandingDocumentReceived,
                documentMarkedInprogress().withDocumentId(documentId).build()));

        final UpdateDocumentStatus updateDocumentStatus = updateDocumentStatus().withDocumentId(documentId).withStatus(OUTSTANDING).build();

        final Envelope<UpdateDocumentStatus> updateDocumentStatusEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.update-document-status"),
                updateDocumentStatus);

        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);

        documentCaseHandler.updateDocumentStatus(updateDocumentStatusEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-outstanding"),
                        payloadIsJson(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        ))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-status-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString())),
                                withJsonPath("$.status", equalTo(OUTSTANDING.toString()))
                        )))
                        .thatMatchesSchema()
                )
        );
    }

    @Test
    public void shouldFailToUpdateDocumentStatusToOutstandingIfNotInProgress() throws Exception {

        final UUID commandId = randomUUID();
        final UUID documentId = randomUUID();
        final EventStream eventStream = mock(EventStream.class);

        final OutstandingDocumentReceived outstandingDocumentReceived = outstandingDocumentReceived()
                .withOutstandingDocument(document()
                        .withScanDocumentId(documentId)
                        .withStatus(OUTSTANDING)
                        .build())
                .build();

        final QueueDocument queueDocument = new QueueDocument();
        queueDocument.apply(outstandingDocumentReceived);

        final UpdateDocumentStatus updateDocumentStatus = updateDocumentStatus().withDocumentId(documentId).withStatus(OUTSTANDING).build();

        final Envelope<UpdateDocumentStatus> updateDocumentStatusEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.update-document-status"),
                updateDocumentStatus);

        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);

        documentCaseHandler.updateDocumentStatus(updateDocumentStatusEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-status-update-failed"),
                        payloadIsJson(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        ))
                        .thatMatchesSchema()
        ));
    }

    @Test
    public void shouldUpdateDocumentStatusToDeleted() throws Exception {

        final UUID commandId = randomUUID();
        final UUID documentId = randomUUID();
        final EventStream eventStream = mock(EventStream.class);

        final OutstandingDocumentReceived outstandingDocumentReceived = outstandingDocumentReceived()
                .withOutstandingDocument(document()
                        .withScanDocumentId(documentId)
                        .withStatus(OUTSTANDING)
                        .build())
                .build();

        final QueueDocument queueDocument = new QueueDocument();
        queueDocument.apply(Stream.of(
                outstandingDocumentReceived,
                documentMarkedInprogress().withDocumentId(documentId).build(),
                documentMarkedCompleted().withDocumentId(documentId).build()));

        final UpdateDocumentStatus updateDocumentStatus = updateDocumentStatus().withDocumentId(documentId).withStatus(DELETED).build();

        final Envelope<UpdateDocumentStatus> updateDocumentStatusEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.update-document-status"),
                updateDocumentStatus);

        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);

        documentCaseHandler.updateDocumentStatus(updateDocumentStatusEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-deleted"),
                        payloadIsJson(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        ))
                        .thatMatchesSchema()
                )
        );
    }

    @Test
    public void shouldLinkDocumentToTheCase() throws Exception {

        final UUID scanDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final String fileName = "theFile.pdf";
        final EventStream eventStream = mock(EventStream.class);
        final CPPCase cppCase = new CPPCase();

        final LinkDocumentToCase linkDocumentToCase = LinkDocumentToCase.linkDocumentToCase()
                .withDocument(document()
                        .withScanDocumentId(scanDocumentId)
                        .withCaseId(caseId)
                        .withStatus(OUTSTANDING)
                        .withFileName(fileName)
                        .build())
                .build();

        final UUID commandId = randomUUID();
        final Envelope<LinkDocumentToCase> linkDocumentToCaseEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.link-document-to-case"),
                linkDocumentToCase);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CPPCase.class)).thenReturn(cppCase);

        documentCaseHandler.linkOutstandingDocumentsToCase(linkDocumentToCaseEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-linked-to-case"),
                        payloadIsJson(allOf(
                                withJsonPath("$.document.scanDocumentId", equalTo(scanDocumentId.toString())),
                                withJsonPath("$.document.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.document.status", equalTo(OUTSTANDING.toString())),
                                withJsonPath("$.document.fileName", equalTo(fileName))
                        )))
                        .thatMatchesSchema()
                )
        );
    }

    @Test
    public void shouldNotLinkDocumentToTheCase() throws Exception {

        final UUID scanDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final String fileName = "theFile.pdf";
        final EventStream eventStream = mock(EventStream.class);
        final CPPCase cppCase = new CPPCase();

        final LinkDocumentToCase linkDocumentToCase = LinkDocumentToCase.linkDocumentToCase()
                .withDocument(document()
                        .withScanDocumentId(scanDocumentId)
                        .withCaseId(caseId)
                        .withStatus(OUTSTANDING)
                        .withFileName(fileName)
                        .build())
                .build();

        final UUID commandId = randomUUID();
        final Envelope<LinkDocumentToCase> linkDocumentToCaseEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.link-document-to-case"),
                linkDocumentToCase);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CPPCase.class)).thenReturn(cppCase);

        cppCase.updateCaseStatus(CaseStatus.EJECTED, caseId);
        documentCaseHandler.linkOutstandingDocumentsToCase(linkDocumentToCaseEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-not-linked-to-case"),
                        payloadIsJson(allOf(
                                withJsonPath("$.document.scanDocumentId", equalTo(scanDocumentId.toString())),
                                withJsonPath("$.document.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.document.status", equalTo(OUTSTANDING.toString())),
                                withJsonPath("$.document.fileName", equalTo(fileName)),
                                withJsonPath("$.reason", equalTo("The document is not linked to the case as the status of the case is EJECTED"))
                        )))
                        .thatMatchesSchema()
                )
        );

    }

    @Test
    public void shouldRecordCaseStatusAsEjectedWhenEjected() throws Exception {
        final UUID documentId = randomUUID();
        final UUID commandId = randomUUID();
        final EventStream documentEventStream = shouldRecordCaseStatus(CaseStatus.EJECTED, "documentqueue.event.case-marked-ejected", documentId, commandId, true);

        assertThat(verifyAppendAndGetArgumentFrom(documentEventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-completed"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        )))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-status-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString())),
                                withJsonPath("$.status", equalTo(COMPLETED.toString()))
                        )))
                        .thatMatchesSchema()
        ));

    }

    @Test
    public void shouldRecordCaseStatusAsFilteredWhenFiltered() throws Exception {
        final UUID commandId = randomUUID();
        final UUID documentId = randomUUID();
        final EventStream documentEventStream = shouldRecordCaseStatus(CaseStatus.FILTERED, "documentqueue.event.case-marked-filtered", documentId, commandId, true);

        assertThat(verifyAppendAndGetArgumentFrom(documentEventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-deleted"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        )))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-delete-from-file-store-requested"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        )))
                        .thatMatchesSchema()
        ));
    }

    @Test
    public void shouldRecordCaseStatusAsSubmissionSucceededWhenSubmissionSucceeded() throws Exception {
        final UUID documentId = randomUUID();
        final UUID commandId = randomUUID();
        shouldRecordCaseStatus(CaseStatus.COMPLETED, "documentqueue.event.case-marked-submission-succeeded", documentId, commandId, false);
    }

    public EventStream shouldRecordCaseStatus(final CaseStatus caseStatus,
                                              final String eventName,
                                              final UUID documentId,
                                              final UUID commandId, boolean mockDocumentStream) throws Exception {

        final UUID caseId = randomUUID();
        final EventStream caseEventStream = mock(EventStream.class);
        final CPPCase cppCase = new CPPCase();
        final EventStream documentEventStream = mock(EventStream.class);
        final QueueDocument queueDocument = new QueueDocument();

        // prepare the command
        final RecordCaseStatus recordCaseStatus = recordCaseStatus()
                .withCaseId(caseId)
                .withStatus(caseStatus)
                .build();

        final Envelope<RecordCaseStatus> recordCaseStatusEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.record-case-status"),
                recordCaseStatus);

        when(eventSource.getStreamById(caseId)).thenReturn(caseEventStream);
        when(aggregateService.get(caseEventStream, CPPCase.class)).thenReturn(cppCase);

        if(mockDocumentStream) {
            when(eventSource.getStreamById(documentId)).thenReturn(documentEventStream);
            when(aggregateService.get(documentEventStream, QueueDocument.class)).thenReturn(queueDocument);
        }


        final Document document = Document.document()
                .withScanDocumentId(documentId)
                .withFileServiceId(UUID.randomUUID())
                .withStatus(OUTSTANDING)
                .build();
        cppCase.linkDocumentToCase(document);
        queueDocument.receiveOutstandingDocument(document);

        documentCaseHandler.recordCaseStatus(recordCaseStatusEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(caseEventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName(eventName),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString()))
                        )))
                        .thatMatchesSchema()
        ));

        return documentEventStream;
    }

    @Test
    public void shouldDeleteDocumentsOfCases() throws Exception {

        final DeleteDocuments deleteDocumentsAggregate = new DeleteDocuments();
        final EventStream deleteDocumentsEventStream = mock(EventStream.class);
        when(eventSource.getStreamById(DELETE_DOCUMENTS_STEAM_ID)).thenReturn(deleteDocumentsEventStream);
        when(aggregateService.get(deleteDocumentsEventStream, DeleteDocuments.class)).thenReturn(deleteDocumentsAggregate);

        final DeleteDocumentsOfCases deleteDocumentsOfCases = DeleteDocumentsOfCases
                .deleteDocumentsOfCases()
                .withCasePTIUrns(asList("urn1", "urn2"))
                .build();

        final UUID commandId = randomUUID();
        final Envelope<DeleteDocumentsOfCases> deleteDocumentOfCasesEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.record-case-status"),
                deleteDocumentsOfCases);
        documentCaseHandler.deleteDocumentsOfCases(deleteDocumentOfCasesEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(deleteDocumentsEventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.delete-documents-of-cases-requested"),
                        payloadIsJson(allOf(
                                withJsonPath("$.casePTIUrns[0]", equalTo("urn1")),
                                withJsonPath("$.casePTIUrns[1]", equalTo("urn2"))
                        )))
                        .thatMatchesSchema()));
    }

    @Test
    public void shouldMarkDocumentsDeletedForCases() throws EventStreamException {
        // case aggregate
        final CPPCase cppCase = new CPPCase();

        // DeleteDocuments
        final DeleteDocuments deleteDocumentsAggregate = new DeleteDocuments();
        final EventStream deleteDocumentsEventStream = mock(EventStream.class);
        when(eventSource.getStreamById(DELETE_DOCUMENTS_STEAM_ID)).thenReturn(deleteDocumentsEventStream);
        when(aggregateService.get(deleteDocumentsEventStream, DeleteDocuments.class)).thenReturn(deleteDocumentsAggregate);

        // document 1 with file service id
        final UUID documentId1 = UUID.randomUUID();
        final UUID fileServiceId1 = UUID.randomUUID();
        final EventStream documentEventStream1 = mock(EventStream.class);
        final Document document1 = Document.document()
                .withScanDocumentId(documentId1)
                .withFileServiceId(fileServiceId1)
                .withStatus(OUTSTANDING).build();
        cppCase.linkDocumentToCase(document1);
        final QueueDocument queueDocumentAggregate1 = new QueueDocument();
        queueDocumentAggregate1.receiveOutstandingDocument(document1);
        when(eventSource.getStreamById(documentId1)).thenReturn(documentEventStream1);
        when(aggregateService.get(documentEventStream1, QueueDocument.class)).thenReturn(queueDocumentAggregate1);

        // document 2 with NO file service id
        final UUID documentId2 = UUID.randomUUID();
        final EventStream documentEventStream2 = mock(EventStream.class);
        final Document document2 = Document.document()
                .withScanDocumentId(documentId2)
                .withStatus(OUTSTANDING).build();
        cppCase.linkDocumentToCase(document2);
        final QueueDocument queueDocumentAggregate2 = new QueueDocument();
        queueDocumentAggregate2.receiveOutstandingDocument(document2);
        when(eventSource.getStreamById(documentId2)).thenReturn(documentEventStream2);
        when(aggregateService.get(documentEventStream2, QueueDocument.class)).thenReturn(queueDocumentAggregate2);

        // command payload
        final UUID caseId = UUID.randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase
                .prosecutionCase()
                .withCasePTIUrn("urn1")
                .withCaseId(caseId)
                .withDocumentIds(asList(documentId1, documentId2))
                .build();
        final MarkDocumentsDeletedForCases markDocumentsDeletedForCases =
                MarkDocumentsDeletedForCases.markDocumentsDeletedForCases()
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .build();

        // command envelope
        final UUID commandId = randomUUID();
        final Envelope<MarkDocumentsDeletedForCases> markDocumentDeletedForCasesEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.mark-documents-deleted-for-cases"),
                markDocumentsDeletedForCases);

        // when
        documentCaseHandler.markDocumentsDeletedForCases(markDocumentDeletedForCasesEnvelope);

        // assert the events population on the delete documents aggregate
        assertThat(verifyAppendAndGetArgumentFrom(deleteDocumentsEventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.delete-documents-of-cases-actioned"),
                        payloadIsJson(allOf(
                                withJsonPath("$.prosecutionCases[0].caseId", equalTo(caseId.toString())),
                                withJsonPath("$.prosecutionCases[0].casePTIUrn", equalTo("urn1")),
                                withJsonPath("$.prosecutionCases[0].documentDeleteStatusList[0].documentId", equalTo(documentId1.toString())),
                                withJsonPath("$.prosecutionCases[0].documentDeleteStatusList[0].deleteStatus", equalTo("Yes")),
                                withJsonPath("$.prosecutionCases[0].documentDeleteStatusList[1].documentId", equalTo(documentId2.toString())),
                                withJsonPath("$.prosecutionCases[0].documentDeleteStatusList[1].deleteStatus", equalTo("No"))
                        )))
                        .thatMatchesSchema()
        ));

        // assert the events populated on queue document aggregate
        assertThat(verifyAppendAndGetArgumentFrom(documentEventStream1), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-deleted"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId1.toString()))
                        )))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-delete-from-file-store-requested"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId1.toString())),
                                withJsonPath("$.fileServiceId", equalTo(fileServiceId1.toString()))
                        ))).thatMatchesSchema()
        ));

        // assert the events populated on queue document aggregate 2
        assertThat(verifyAppendAndGetArgumentFrom(documentEventStream2), not(streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-deleted"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId2.toString()))
                        )))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-delete-from-file-store-requested"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId2.toString()))
                        ))).thatMatchesSchema()
        )));
    }

    @Test
    public void shouldMarkDocumentDeletedFromFileStore() throws Exception {


        final UUID documentId = UUID.randomUUID();
        final UUID fileServiceId = UUID.randomUUID();
        final QueueDocument queueDocumentAggregate = new QueueDocument();
        final CPPCase cppCase = new CPPCase();
        final EventStream queueDocumentEventStream = mock(EventStream.class);
        when(eventSource.getStreamById(documentId)).thenReturn(queueDocumentEventStream);
        when(aggregateService.get(queueDocumentEventStream, QueueDocument.class)).thenReturn(queueDocumentAggregate);

        final MarkDocumentDeletedFromFilestore markDocumentDeletedFromFilestore = MarkDocumentDeletedFromFilestore
                .markDocumentDeletedFromFilestore()
                .withDocumentId(documentId)
                .build();

        final Document document = Document.document()
                .withScanDocumentId(documentId)
                .withFileServiceId(fileServiceId)
                .withStatus(OUTSTANDING).build();
        cppCase.linkDocumentToCase(document);
        queueDocumentAggregate.receiveOutstandingDocument(document);

        final UUID commandId = randomUUID();
        final Envelope<MarkDocumentDeletedFromFilestore> markDocumentDeletedFromFileStoreEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.mark-document-deleted-from-file-store"),
                markDocumentDeletedFromFilestore);

        documentCaseHandler.setMarkDocumentDeletedFromFileStore(markDocumentDeletedFromFileStoreEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(queueDocumentEventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-deleted-from-file-store"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString())),
                                withJsonPath("$.fileServiceId", equalTo(fileServiceId.toString()))
                        )))
                        .thatMatchesSchema()));
    }

    @Test
    public void shouldRemoveDocumentFromQueueWhenTheDocumentSourceIsCPS() throws Exception {

        final UUID commandId = randomUUID();
        final UUID documentId = randomUUID();
        final EventStream eventStream = mock(EventStream.class);

        final ReceiveOutstandingDocument receiveOutstandingDocument = receiveOutstandingDocument()
                .withOutstandingDocument(document()
                        .withScanDocumentId(documentId)
                        .withSource(Source.CPS)
                        .withStatus(OUTSTANDING)
                        .build())
                .build();

        final QueueDocument queueDocument = new QueueDocument();
        queueDocument.receiveOutstandingDocument(receiveOutstandingDocument.getOutstandingDocument());

        final RemoveDocumentFromQueue removeDocumentFromQueue = removeDocumentFromQueue()
                .withDocumentId(documentId)
                .build();

        final Envelope<RemoveDocumentFromQueue> removeDocumentFromQueueEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.remove-document-from-queue"),
                removeDocumentFromQueue);

        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);

        documentCaseHandler.removeDocumentFromQueue(removeDocumentFromQueueEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-marked-completed"),
                        payloadIsJson(
                                withJsonPath("$.documentId", equalTo(documentId.toString()))
                        ))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        metadata()
                                .withCausationIds(commandId)
                                .withName("documentqueue.event.document-status-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.documentId", equalTo(documentId.toString())),
                                withJsonPath("$.status", equalTo(COMPLETED.toString()))
                        )))
                        .thatMatchesSchema()
                )
        );

    }


    @Test
    public void shouldNotRemoveDocumentFromQueueWhenTheDocumentSourceIsNotCPS() throws Exception {

        final UUID commandId = randomUUID();
        final UUID documentId = randomUUID();
        final EventStream eventStream = mock(EventStream.class);

        final ReceiveOutstandingDocument receiveOutstandingDocument = receiveOutstandingDocument()
                .withOutstandingDocument(document()
                        .withScanDocumentId(documentId)
                        .withStatus(OUTSTANDING)
                        .build())
                .build();

        final QueueDocument queueDocument = new QueueDocument();
        queueDocument.receiveOutstandingDocument(receiveOutstandingDocument.getOutstandingDocument());

        final RemoveDocumentFromQueue removeDocumentFromQueue = removeDocumentFromQueue()
                .withDocumentId(documentId)
                .build();

        final Envelope<RemoveDocumentFromQueue> removeDocumentFromQueueEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(commandId)
                        .withName("documentqueue.command.remove-document-from-queue"),
                removeDocumentFromQueue);

        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, QueueDocument.class)).thenReturn(queueDocument);

        documentCaseHandler.removeDocumentFromQueue(removeDocumentFromQueueEnvelope);

        assertThat(eventStream, eventStreamWithEmptyStream());
    }
}