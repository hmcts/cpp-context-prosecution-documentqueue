package uk.gov.moj.cpp.prosecution.documentqueue.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
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
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus.updateDocumentStatus;
import static uk.gov.moj.cpp.documentqueue.event.DocumentMarkedCompleted.documentMarkedCompleted;
import static uk.gov.moj.cpp.documentqueue.event.DocumentMarkedInprogress.documentMarkedInprogress;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument.receiveOutstandingDocument;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.model.CourtDocument;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.DocumentStatusUpdated;
import uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus;
import uk.gov.moj.cpp.documentqueue.event.AttachDocumentRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentAttached;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedCompleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedDeleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedInprogress;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedOutstanding;
import uk.gov.moj.cpp.documentqueue.event.DocumentStatusUpdateFailed;
import uk.gov.moj.cpp.documentqueue.event.OutstandingDocumentReceived;
import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.QueueDocument;

import java.util.Queue;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DocumentCaseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private DocumentCaseHandler documentCaseHandler;

    @Before
    public void setup() throws Exception {
        createEnveloperWithEvents(
                OutstandingDocumentReceived.class,
                DocumentMarkedOutstanding.class,
                DocumentMarkedInprogress.class,
                DocumentMarkedCompleted.class,
                DocumentMarkedDeleted.class,
                DocumentStatusUpdated.class,
                DocumentStatusUpdateFailed.class,
                AttachDocumentRequested.class);
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


        final OutstandingDocumentReceived outstandingDocumentReceived = OutstandingDocumentReceived.outstandingDocumentReceived()
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

        final OutstandingDocumentReceived outstandingDocumentReceived = OutstandingDocumentReceived.outstandingDocumentReceived()
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

        final OutstandingDocumentReceived outstandingDocumentReceived = OutstandingDocumentReceived.outstandingDocumentReceived()
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

        final OutstandingDocumentReceived outstandingDocumentReceived = OutstandingDocumentReceived.outstandingDocumentReceived()
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

        final OutstandingDocumentReceived outstandingDocumentReceived = OutstandingDocumentReceived.outstandingDocumentReceived()
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

        final OutstandingDocumentReceived outstandingDocumentReceived = OutstandingDocumentReceived.outstandingDocumentReceived()
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
}
