package uk.gov.moj.cpp.prosecution.documentqueue.event.listener;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.documentqueue.event.matcher.ScanDocumentMatcher.matchesNonNullPropertiesOfDocument;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedSubmissionSucceeded;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedCompleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedDeleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedInprogress;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedOutstanding;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.event.service.CaseStatusService;
import uk.gov.moj.cpp.prosecution.documentqueue.event.service.DocumentService;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentStatusUpdatedListenerTest {

    @InjectMocks
    DocumentStatusUpdatedListener documentStatusUpdatedListener;

    @Mock
    DocumentService documentService;

    @Mock
    CaseStatusService caseStatusService;

    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    @Captor
    private ArgumentCaptor<CaseStatus> caseStatusCaptor;

    @Test
    public void handleDocumentMarkedDeleted() throws IOException {
        final Envelope<DocumentMarkedDeleted> documentMarkedDeletedEnvelope = mock(Envelope.class);
        DocumentMarkedDeleted documentMarkedDeleted = mock(DocumentMarkedDeleted.class);
        final UUID exampleDocumentId = UUID.fromString("285256e4-23df-475c-87ac-c0dc031349bf");
        final Document dummyDocument = getDummyDocument();
        final ZonedDateTime eventDateTime = ZonedDateTime.now(UTC).truncatedTo(ChronoUnit.MILLIS);
        final Metadata metadata = metadataWithRandomUUID("documentqueue.event.document-marked-completed")
                .createdAt(eventDateTime).build();

        when(documentMarkedDeletedEnvelope.payload()).thenReturn(documentMarkedDeleted);
        when(documentMarkedDeletedEnvelope.metadata()).thenReturn(metadata);
        when(documentMarkedDeleted.getDocumentId()).thenReturn(exampleDocumentId);
        when(documentService.getDocumentByDocumentId(exampleDocumentId)).thenReturn(dummyDocument);

        final Document expectedDocument = new Document.DocumentBuilder().withDocument(dummyDocument)
                .withStatus(Status.DELETED)
                .withStatusUpdatedDate(eventDateTime)
                .withLastModified(eventDateTime)
                .build();

        documentStatusUpdatedListener.handleDocumentMarkedAsDeleted(documentMarkedDeletedEnvelope);
        verify(documentService, times(1)).saveDocument(documentCaptor.capture());

        final Document document = documentCaptor.getValue();
        assertThat(document, matchesNonNullPropertiesOfDocument(expectedDocument));
    }

    @Test
    public void handleDocumentMarkedInProgress() throws IOException {
        final Envelope<DocumentMarkedInprogress> documentMarkedInProgressEnvelope = mock(Envelope.class);
        DocumentMarkedInprogress documentMarkedInprogress = mock(DocumentMarkedInprogress.class);
        final UUID exampleDocumentId = UUID.fromString("285256e4-23df-475c-87ac-c0dc031349bf");
        final Document dummyDocument = getDummyDocument();
        final ZonedDateTime eventDateTime = ZonedDateTime.now(UTC).truncatedTo(ChronoUnit.MILLIS);
        final Metadata metadata = metadataWithRandomUUID("documentqueue.event.document-marked-inprogress")
                .createdAt(eventDateTime).build();

        when(documentMarkedInProgressEnvelope.payload()).thenReturn(documentMarkedInprogress);
        when(documentMarkedInProgressEnvelope.metadata()).thenReturn(metadata);
        when(documentMarkedInprogress.getDocumentId()).thenReturn(exampleDocumentId);
        when(documentService.getDocumentByDocumentId(exampleDocumentId)).thenReturn(dummyDocument);

        final Document expectedDocument = new Document.DocumentBuilder().withDocument(dummyDocument)
                .withStatus(Status.IN_PROGRESS)
                .withStatusUpdatedDate(eventDateTime)
                .withLastModified(eventDateTime)
                .build();

        documentStatusUpdatedListener.handleDocumentMarkedInProgress(documentMarkedInProgressEnvelope);
        verify(documentService, times(1)).saveDocument(documentCaptor.capture());

        final Document document = documentCaptor.getValue();
        assertThat(document, matchesNonNullPropertiesOfDocument(expectedDocument));
    }

    @Test
    public void handleDocumentMarkedCompleted() throws IOException {
        final Envelope<DocumentMarkedCompleted> documentMarkedCompletedEnvelope = mock(Envelope.class);
        DocumentMarkedCompleted documentMarkedCompleted = mock(DocumentMarkedCompleted.class);
        final UUID exampleDocumentId = UUID.fromString("285256e4-23df-475c-87ac-c0dc031349bf");
        final Document dummyDocument = getDummyDocument();
        final ZonedDateTime eventDateTime = ZonedDateTime.now(UTC).truncatedTo(ChronoUnit.MILLIS);
        final Metadata metadata = metadataWithRandomUUID("documentqueue.event.document-marked-completed")
                .createdAt(eventDateTime).build();

        when(documentMarkedCompletedEnvelope.payload()).thenReturn(documentMarkedCompleted);
        when(documentMarkedCompletedEnvelope.metadata()).thenReturn(metadata);
        when(documentMarkedCompleted.getDocumentId()).thenReturn(exampleDocumentId);
        when(documentService.getDocumentByDocumentId(exampleDocumentId)).thenReturn(dummyDocument);

        final Document expectedDocument = new Document.DocumentBuilder().withDocument(dummyDocument)
                .withStatus(Status.COMPLETED)
                .withStatusUpdatedDate(eventDateTime)
                .withLastModified(eventDateTime)
                .build();

        documentStatusUpdatedListener.handleDocumentMarkedCompleted(documentMarkedCompletedEnvelope);
        verify(documentService, times(1)).saveDocument(documentCaptor.capture());

        final Document document = documentCaptor.getValue();
        assertThat(document, matchesNonNullPropertiesOfDocument(expectedDocument));
    }

    @Test
    public void handleDocumentCaseMarkedSubmissionSucceeded() throws IOException {
        final Envelope<CaseMarkedSubmissionSucceeded> documentMarkedCompletedEnvelope = mock(Envelope.class);
        CaseMarkedSubmissionSucceeded documentCaseMarkedSubmissionSucceeded = mock(CaseMarkedSubmissionSucceeded.class);
        final UUID exampleCaseId = UUID.fromString("285256e4-23df-475c-87ac-c0dc031349bf");

        final ZonedDateTime eventDateTime = ZonedDateTime.now(UTC).truncatedTo(ChronoUnit.MILLIS);

        when(documentMarkedCompletedEnvelope.payload()).thenReturn(documentCaseMarkedSubmissionSucceeded);
        when(documentCaseMarkedSubmissionSucceeded.getCaseId()).thenReturn(exampleCaseId);
        doNothing().when(caseStatusService).saveCaseStatus(any(CaseStatus.class));

        final CaseStatus expectedCaseStatus = new CaseStatus.CaseStatusBuilder()
                .withStatus(Status.COMPLETED)
                .withCaseId(exampleCaseId)
                .withId(UUID.randomUUID())
                .build();
        documentStatusUpdatedListener.handleDocumentCaseMarkedSubmissionSucceeded(documentMarkedCompletedEnvelope);
        verify(caseStatusService, times(1)).saveCaseStatus(caseStatusCaptor.capture());

        final CaseStatus caseStatus = caseStatusCaptor.getValue();
        assertThat(caseStatus.getStatus(), equalTo(expectedCaseStatus.getStatus()));
        assertThat(caseStatus.getCaseId(), equalTo(expectedCaseStatus.getCaseId()));
    }

    @Test
    public void handleDocumentMarkedOutstanding() throws IOException {
        final Envelope<DocumentMarkedOutstanding> documentMarkedOutstandingEnvelope = mock(Envelope.class);
        DocumentMarkedOutstanding documentMarkedOutstanding = mock(DocumentMarkedOutstanding.class);
        final UUID exampleDocumentId = UUID.fromString("285256e4-23df-475c-87ac-c0dc031349bf");
        final Document dummyDocument = getDummyDocument();
        final ZonedDateTime eventDateTime = ZonedDateTime.now(UTC).truncatedTo(ChronoUnit.MILLIS);
        final Metadata metadata = metadataWithRandomUUID("documentqueue.event.document-marked-completed")
                .createdAt(eventDateTime).build();

        when(documentMarkedOutstandingEnvelope.payload()).thenReturn(documentMarkedOutstanding);
        when(documentMarkedOutstandingEnvelope.metadata()).thenReturn(metadata);
        when(documentMarkedOutstanding.getDocumentId()).thenReturn(exampleDocumentId);
        when(documentService.getDocumentByDocumentId(exampleDocumentId)).thenReturn(dummyDocument);

        final Document expectedDocument = new Document.DocumentBuilder().withDocument(dummyDocument)
                .withStatus(Status.OUTSTANDING)
                .withStatusUpdatedDate(eventDateTime)
                .withLastModified(eventDateTime)
                .build();

        documentStatusUpdatedListener.handleDocumentMarkedOutstanding(documentMarkedOutstandingEnvelope);
        verify(documentService, times(1)).saveDocument(documentCaptor.capture());

        final Document document = documentCaptor.getValue();
        assertThat(document, matchesNonNullPropertiesOfDocument(expectedDocument));
    }

    private Document getDummyDocument() {
        return new Document.DocumentBuilder()
                .withId(UUID.fromString("c78cb222-58d0-42ac-9cc8-d1b2710171bf"))
                .withCaseUrn("TVL6794003OMW")
                .withProsecutorAuthorityId("GAEAA01")
                .withProsecutorAuthorityCode("TVL")
                .withDocumentControlNumber("54856456")
                .withDocumentName("SJPMC100")
                .withFileName("financial_means_with_ocr_data.pdf")
                .withManualIntervention("No")
                .withVendorReceivedDate(ZonedDateTime.parse("2020-02-12T20:11:32.013Z"))
                .withScanningDate(ZonedDateTime.parse("2020-02-12T20:51:32.013Z"))
                .withNotes("Some test notes")
                .withEnvelopeId(UUID.fromString("285256e4-23df-475c-87ac-c0dc031349bf"))
                .withZipFileName("bae5ffba-80e6-4baa-9a10-8a88460e941f.zip")
                .withSource(Source.BULKSCAN)
                .withStatus(OUTSTANDING)
                .withType(Type.APPLICATION)
                .withStatusUpdatedDate(ZonedDateTime.parse("2020-02-13T20:51:32.013Z"))
                .build();
    }
}
