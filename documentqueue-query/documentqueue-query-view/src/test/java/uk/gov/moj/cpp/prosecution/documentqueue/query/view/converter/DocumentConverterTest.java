package uk.gov.moj.cpp.prosecution.documentqueue.query.view.converter;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Source.BULKSCAN;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Type.APPLICATION;

import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentConverterTest {

    @InjectMocks
    private DocumentConverter documentConverter;

    @Test
    public void shouldConvertDocumentsIntoScanDocuments() {

        final UUID documentId_1 = randomUUID();
        final UUID documentId_2 = randomUUID();
        final UUID envelopeId_1 = randomUUID();
        final UUID envelopeId_2 = randomUUID();
        final ZonedDateTime now_1 = ZonedDateTime.now(UTC);
        final ZonedDateTime now_2 = ZonedDateTime.now(UTC).plusDays(1L);

        final Document document_1 = new Document.DocumentBuilder()
                .withCaseUrn("caseUrn")
                .withId(documentId_1)
                .withEnvelopeId(envelopeId_1)
                .withType(APPLICATION)
                .withFileName("fileName_1")
                .withProsecutorAuthorityCode("prosecutorAuthorityCode_1")
                .withScanningDate(now_1)
                .withVendorReceivedDate(now_1)
                .withSource(BULKSCAN)
                .withStatus(OUTSTANDING)
                .withStatusUpdatedDate(now_1)
                .build();

        final Document document_2 = new Document.DocumentBuilder()
                .withCaseUrn("caseUrn")
                .withId(documentId_2)
                .withEnvelopeId(envelopeId_2)
                .withType(APPLICATION)
                .withFileName("fileName_2")
                .withProsecutorAuthorityCode("prosecutorAuthorityCode_2")
                .withScanningDate(now_2)
                .withVendorReceivedDate(now_2)
                .withSource(BULKSCAN)
                .withStatus(OUTSTANDING)
                .withStatusUpdatedDate(now_2)
                .build();

        final List<ScanDocument> scanDocuments = documentConverter.convertToScanDocuments(asList(document_1, document_2));

        assertThat(scanDocuments.size(), is(2));

        final ScanDocument scanDocument_1 = scanDocuments.get(0);

        assertThat(scanDocument_1.getCaseUrn(), is("caseUrn"));
        assertThat(scanDocument_1.getDocumentId(), is(documentId_1));
        assertThat(scanDocument_1.getEnvelopeId(), is(envelopeId_1));
        assertThat(scanDocument_1.getDocumentType(), is(APPLICATION));
        assertThat(scanDocument_1.getFileName(), is("fileName_1"));
        assertThat(scanDocument_1.getProsecutorAuthorityCode(), is("prosecutorAuthorityCode_1"));
        assertThat(scanDocument_1.getReceivedDate(), is(now_1));
        assertThat(scanDocument_1.getSource(), is(BULKSCAN));
        assertThat(scanDocument_1.getStatus(), is(OUTSTANDING));
        assertThat(scanDocument_1.getStatusUpdatedDate(), is(now_1));

        final ScanDocument scanDocument_2 = scanDocuments.get(1);

        assertThat(scanDocument_2.getCaseUrn(), is("caseUrn"));
        assertThat(scanDocument_2.getDocumentId(), is(documentId_2));
        assertThat(scanDocument_2.getEnvelopeId(), is(envelopeId_2));
        assertThat(scanDocument_2.getDocumentType(), is(APPLICATION));
        assertThat(scanDocument_2.getFileName(), is("fileName_2"));
        assertThat(scanDocument_2.getProsecutorAuthorityCode(), is("prosecutorAuthorityCode_2"));
        assertThat(scanDocument_2.getReceivedDate(), is(now_2));
        assertThat(scanDocument_2.getSource(), is(BULKSCAN));
        assertThat(scanDocument_2.getStatus(), is(OUTSTANDING));
        assertThat(scanDocument_1.getStatusUpdatedDate(), is(now_1));
    }

    @Test
    public void shouldConvertDocumentIntoDocumentContentView() {

        final UUID documentId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();

        final Document document = new Document.DocumentBuilder()
                .withId(documentId)
                .withSource(BULKSCAN)
                .withStatus(OUTSTANDING)
                .withType(APPLICATION)
                .withFileName("fileName")
                .withMaterialId(materialId)
                .withFileServiceId(fileServiceId)
                .build();

        final DocumentContentView documentContentView = documentConverter.convertToDocumentContentView(document);

        assertThat(documentContentView.getDocumentId(), is(documentId));
        assertThat(documentContentView.getSource(), is(BULKSCAN));
        assertThat(documentContentView.getStatus(), is(OUTSTANDING));
        assertThat(documentContentView.getType(), is(APPLICATION));
        assertThat(documentContentView.getFileName(), is("fileName"));
        assertThat(documentContentView.getMaterialId(), is(materialId));
        assertThat(documentContentView.getFileServiceId(), is(fileServiceId));
    }
}
