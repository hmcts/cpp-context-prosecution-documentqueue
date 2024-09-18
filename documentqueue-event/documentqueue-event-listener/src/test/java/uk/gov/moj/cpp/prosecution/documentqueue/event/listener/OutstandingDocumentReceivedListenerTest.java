package uk.gov.moj.cpp.prosecution.documentqueue.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.parse;
import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Source.BULKSCAN;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Type.APPLICATION;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.documentqueue.event.matcher.ScanDocumentMatcher.matchesNonNullPropertiesOfDocument;
import static util.TestUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.DocumentConverter;
import uk.gov.moj.cpp.prosecution.documentqueue.event.service.DocumentService;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OutstandingDocumentReceivedListenerTest {

    @InjectMocks
    private OutstandingDocumentReceivedListener outstandingDocumentReceivedListener;

    @Spy
    private DocumentConverter documentConverter;

    @Mock
    private DocumentService documentService;

    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    @Test
    public void testOutstandingDocumentReceivedEvent() throws IOException {
        final ZonedDateTime eventDateTime = ZonedDateTime.now(UTC).truncatedTo(ChronoUnit.MILLIS);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("documentqueue.event.outstanding-document-received").createdAt(eventDateTime).build(),
                givenPayload("/documentqueue.event.outstanding-document-received.json")
        );

        final Document expectedDocument = new Document.DocumentBuilder().withId(fromString("c78cb222-58d0-42ac-9cc8-d1b2710171bf"))
                .withCaseUrn("TVL6794003OMW")
                .withDocumentControlNumber("54856456")
                .withDocumentName("SJPMC100")
                .withEnvelopeId(fromString("285256e4-23df-475c-87ac-c0dc031349bf"))
                .withFileName("financial_means_with_ocr_data.pdf")
                .withManualIntervention("No")
                .withNotes("These are file notes for testing")
                .withProsecutorAuthorityCode("TVL")
                .withProsecutorAuthorityId("GAEAA01")
                .withScanningDate(parse("2020-02-12T20:51:32.013Z"))
                .withSource(BULKSCAN)
                .withStatus(OUTSTANDING)
                .withStatusUpdatedDate(parse("2020-02-12T20:51:32.013Z"))
                .withType(APPLICATION)
                .withVendorReceivedDate(parse("2020-02-12T20:11:32.013Z"))
                .withZipFileName("bae5ffba-80e6-4baa-9a10-8a88460e941f.zip")
                .withLastModified(eventDateTime)
                .build();

        outstandingDocumentReceivedListener.handleOutstandingDocumentReceived(envelope);

        verify(documentService, times(1)).saveDocument(documentCaptor.capture());

        final Document document = documentCaptor.getValue();
        assertThat(document, matchesNonNullPropertiesOfDocument(expectedDocument));
    }

}
