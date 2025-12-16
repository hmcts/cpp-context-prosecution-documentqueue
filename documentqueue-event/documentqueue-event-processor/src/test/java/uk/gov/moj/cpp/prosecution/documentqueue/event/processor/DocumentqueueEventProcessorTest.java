package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentIdsOfCases;
import uk.gov.justice.cpp.prosecution.documentqueue.domain.ProsecutionCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.documentqueue.command.handler.RemoveDocumentFromQueue;
import uk.gov.moj.cpp.documentqueue.command.handler.UpdateDocumentStatus;
import uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesRequested;
import uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequestReceived;
import uk.gov.moj.cpp.documentqueue.event.DocumentDeleteFromFileStoreRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentLinkedToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDeleteExpiredDocumentsAsRequested;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDocumentDeletedFromFilestore;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.MarkDocumentsDeletedForCases;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.DocumentQueryView;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class DocumentqueueEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private Logger logger;

    @Mock
    private FileService fileService;

    @InjectMocks
    private DocumentqueueEventProcessor documentqueueEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope> argumentCaptor;

    @Captor
    private ArgumentCaptor<UUID>  fileServiceCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> documentViewCapture1;

    @Captor
    private ArgumentCaptor<JsonEnvelope> documentViewCapture2;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private DocumentQueryView documentQueryView;

    private final String documentExpiryDays = "28";

    private final String documentFileServiceDeleteDays = "90";

    private final String documentFileServiceDeleteLimit = "300";

    @BeforeEach
    public void setUp() {
        ReflectionUtil.setField(this.documentqueueEventProcessor, "documentExpiryDays", documentExpiryDays);
        ReflectionUtil.setField(this.documentqueueEventProcessor, "documentFileServiceDeleteDays", documentFileServiceDeleteDays);
        ReflectionUtil.setField(this.documentqueueEventProcessor, "documentFileServiceDeleteLimit", documentFileServiceDeleteLimit);
    }

    @Test
    public void shouldSendPublicDocumentStatusUpdated() {
        final var argumentCaptor = ArgumentCaptor.forClass(DefaultEnvelope.class);
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
        verify(sender).send(argumentCaptor.capture());

        final Envelope<JsonValue> jsonValueEnvelope = argumentCaptor.getValue();

        assertThat(jsonValueEnvelope.metadata(), withMetadataEnvelopedFrom(eventEnvelope).withName("public.documentqueue.document-status-updated"));
        assertThat(jsonValueEnvelope.payload(), payloadIsJson(allOf(
                withJsonPath("$.documentId", equalTo(documentId)),
                withJsonPath("$.status", equalTo(IN_PROGRESS.toString()))
        )));
    }

    @Test
    public void shouldMarkDocumentCompleted() {
        final var argumentCaptor = ArgumentCaptor.forClass(DefaultEnvelope.class);
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

        final DefaultEnvelope<JsonObject> jsonObjectEnvelope = argumentCaptor.getValue();

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
        final var argumentCaptor = ArgumentCaptor.forClass(DefaultEnvelope.class);
        // given
        final UUID prosecutionCaseId = randomUUID();
        final uk.gov.justice.prosecution.documentqueue.domain.Document document1 = mock(uk.gov.justice.prosecution.documentqueue.domain.Document.class);

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("documentqueue.event.document-linked-to-case")
                .build();
        final Envelope<DocumentLinkedToCase> envelope = envelopeFrom(metadata, documentLinkedToCase().withDocument(document1).build());

        // when the ejected event is processed
        documentqueueEventProcessor.processDocumentLinkedToCase(envelope);

        // then fire a command record the case status
        verify(sender).send(argumentCaptor.capture());
        final DefaultEnvelope<ReceiveOutstandingDocument> receiveOutstandingDocumentEnvelope = argumentCaptor.getValue();

        final ReceiveOutstandingDocument receiveOutstandingDocument = receiveOutstandingDocumentEnvelope.payload();
        final Metadata metadataOfSendRequest = receiveOutstandingDocumentEnvelope.metadata();
        assertThat(receiveOutstandingDocument.getOutstandingDocument(), is(document1));
        assertThat(metadataOfSendRequest.name(), is("documentqueue.command.receive-outstanding-document"));
    }

    @Test
    public void shouldProcessDeleteDocumentsOfCasesRequested() {
        final UUID caseId = UUID.randomUUID();
        final UUID documentId = UUID.randomUUID();
        // given
        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("documentqueue.event.delete-documents-of-cases-requested")
                .build();
        final Envelope<DeleteDocumentsOfCasesRequested> envelope =
                envelopeFrom(metadata,
                        DeleteDocumentsOfCasesRequested
                                .deleteDocumentsOfCasesRequested()
                                .withCasePTIUrns(asList("urn1"))
                                .build());

        final ProsecutionCase prosecutionCase =
                ProsecutionCase
                        .prosecutionCase()
                        .withDocumentIds(asList(documentId))
                        .withCasePTIUrn("urn1")
                        .withCaseId(caseId)
                        .build();
        final DocumentIdsOfCases documentIdsOfCases =
                DocumentIdsOfCases
                        .documentIdsOfCases()
                        .withProsecutionCases(asList(prosecutionCase))
                        .build();

        when(documentQueryView.getDocumentIdsOfCases(any()))
                .thenReturn(envelopeFrom(metadataFrom(envelope.metadata()), documentIdsOfCases));

        // when
        documentqueueEventProcessor.processDeleteDocumentsOfCasesRequested(envelope);

        // then fire a command record the case status
        verify(sender).send(argumentCaptor.capture());
        final Envelope<MarkDocumentsDeletedForCases> markDocumentsDeletedForCasesEnvelope = argumentCaptor.getValue();

        final MarkDocumentsDeletedForCases markDocumentsDeletedForCases = markDocumentsDeletedForCasesEnvelope.payload();
        final Metadata metadataOfSendRequest = markDocumentsDeletedForCasesEnvelope.metadata();
        assertThat(markDocumentsDeletedForCases.getProsecutionCases().get(0).getCaseId(), is(caseId));
        assertThat(markDocumentsDeletedForCases.getProsecutionCases().get(0).getCasePTIUrn(), is("urn1"));
        assertThat(markDocumentsDeletedForCases.getProsecutionCases().get(0).getDocumentIds().get(0), is(documentId));
        assertThat(metadataOfSendRequest.name(), is("documentqueue.command.mark-documents-deleted-for-cases"));
    }

    @Test
    public void shouldProcessDocumentDeleteFromFileStoreRequested() throws FileServiceException {
        // given
        final UUID documentId = UUID.randomUUID();
        final UUID fileServiceId = UUID.randomUUID();

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("documentqueue.event.document-delete-from-file-store-requested")
                .build();
        final Envelope<DocumentDeleteFromFileStoreRequested> envelope =
                envelopeFrom(metadata,
                        DocumentDeleteFromFileStoreRequested
                                .documentDeleteFromFileStoreRequested()
                                .withDocumentId(documentId)
                                .withFileServiceId(fileServiceId)
                                .build());

        // when
        documentqueueEventProcessor.processDocumentDeleteFromFileStoreRequested(envelope);

        // then
        verify(fileService, times(1)).delete(fileServiceCaptor.capture());
        assertThat(fileServiceCaptor.getValue(), is(fileServiceId));

        verify(sender).send(argumentCaptor.capture());
        final Envelope<MarkDocumentDeletedFromFilestore> markDocumentDeletedFromFilestoreEnvelope = argumentCaptor.getValue();
        final MarkDocumentDeletedFromFilestore markDocumentDeletedFromFilestore = markDocumentDeletedFromFilestoreEnvelope.payload();

        final Metadata metadataOfSendRequest = markDocumentDeletedFromFilestoreEnvelope.metadata();
        assertThat(metadataOfSendRequest.name(), is("documentqueue.command.mark-document-deleted-from-file-store"));
        assertThat(markDocumentDeletedFromFilestore.getDocumentId(), is(documentId));
    }

    @Test
    public void shouldProcessExpiredDocuments() throws FileServiceException {
        // given
        final UUID documentId1 = UUID.randomUUID();
        final UUID documentId2 = UUID.randomUUID();

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("documentqueue.event.delete-expired-documents-request-received")
                .build();
        final Envelope<DeleteExpiredDocumentsRequestReceived> envelope =
                envelopeFrom(metadata,
                        DeleteExpiredDocumentsRequestReceived
                                .deleteExpiredDocumentsRequestReceived()
                                .build());

        // stub queries
        final Document document1 = new Document.DocumentBuilder().document()
                .withFileServiceId(documentId1)
                .withId(documentId1)
                .build();
        final Document document2 = new Document.DocumentBuilder().document()
                .withFileServiceId(documentId1)
                .withId(documentId2)
                .build();

        final Envelope<List<Document>> responseEnvelope = envelopeFrom(metadata, Arrays.asList(document1, document2));
        when(documentQueryView.getExpiredDocuments(any(JsonEnvelope.class))).thenReturn(responseEnvelope);
        when(documentQueryView.getDocumentsEligibleForDeletionFromFileStore(any(JsonEnvelope.class))).thenReturn(responseEnvelope);

        // when
        documentqueueEventProcessor.processDeleteExpiredDocumentsRequestReceived(envelope);

        // then
        verify(documentQueryView, times(1)).getExpiredDocuments(documentViewCapture1.capture());
        final JsonEnvelope queryEnvelope = documentViewCapture1.getValue();
        final Integer expiryDays = queryEnvelope.payloadAsJsonObject().getInt("documentExpiryDays");
        assertThat(expiryDays,equalTo(28));

        verify(documentQueryView, times(1)).getDocumentsEligibleForDeletionFromFileStore(documentViewCapture2.capture());
        final JsonEnvelope queryEnvelope1 = documentViewCapture2.getValue();
        final Integer documentFileServiceDeleteDays = queryEnvelope1.payloadAsJsonObject().getInt("documentFileServiceDeleteDays");
        assertThat(documentFileServiceDeleteDays,equalTo(90));

        verify(fileService, times(2)).delete(fileServiceCaptor.capture());
        fileServiceCaptor.getAllValues();
        assertThat(fileServiceCaptor.getAllValues().get(0), is(documentId1));
        assertThat(fileServiceCaptor.getAllValues().get(1), is(documentId2));

        verify(sender, times(5)).send(argumentCaptor.capture());

        Envelope<RemoveDocumentFromQueue> removeDocumentFromQueueEnvelope = argumentCaptor.getAllValues().get(0);
        RemoveDocumentFromQueue removeDocumentFromQueue = removeDocumentFromQueueEnvelope.payload();
        Metadata metadataOfSendRequest = removeDocumentFromQueueEnvelope.metadata();
        assertThat(metadataOfSendRequest.name(), is("documentqueue.command.remove-document-from-queue"));
        assertThat(removeDocumentFromQueue.getDocumentId(), is(documentId1));

        removeDocumentFromQueueEnvelope = argumentCaptor.getAllValues().get(1);
        removeDocumentFromQueue = removeDocumentFromQueueEnvelope.payload();
        metadataOfSendRequest = removeDocumentFromQueueEnvelope.metadata();
        assertThat(metadataOfSendRequest.name(), is("documentqueue.command.remove-document-from-queue"));
        assertThat(removeDocumentFromQueue.getDocumentId(), is(documentId2));

        final Envelope<MarkDeleteExpiredDocumentsAsRequested>  markDeleteExpiredDocumentsAsRequestedEnvelope =  argumentCaptor.getAllValues().get(2);
        final MarkDeleteExpiredDocumentsAsRequested markDeleteExpiredDocumentsAsRequested = markDeleteExpiredDocumentsAsRequestedEnvelope.payload();
        metadataOfSendRequest = markDeleteExpiredDocumentsAsRequestedEnvelope.metadata();
        assertThat(metadataOfSendRequest.name(), is("documentqueue.command.mark-delete-expired-documents-as-requested"));
        assertThat(markDeleteExpiredDocumentsAsRequested.getDocumentIds().get(0), is(documentId1));
        assertThat(markDeleteExpiredDocumentsAsRequested.getDocumentIds().get(1), is(documentId2));

        Envelope<UpdateDocumentStatus> updateDocumentStatusEnvelope = argumentCaptor.getAllValues().get(3);
        UpdateDocumentStatus updateDocumentStatus = updateDocumentStatusEnvelope.payload();
        Metadata documentStatusMetadata = updateDocumentStatusEnvelope.metadata();
        assertThat(documentStatusMetadata.name(), is("documentqueue.command.update-document-status"));
        assertThat(updateDocumentStatus.getDocumentId(), is(documentId1));

        updateDocumentStatusEnvelope = argumentCaptor.getAllValues().get(4);
        updateDocumentStatus = updateDocumentStatusEnvelope.payload();
        documentStatusMetadata = updateDocumentStatusEnvelope.metadata();
        assertThat(documentStatusMetadata.name(), is("documentqueue.command.update-document-status"));
        assertThat(updateDocumentStatus.getDocumentId(), is(documentId2));
    }

}