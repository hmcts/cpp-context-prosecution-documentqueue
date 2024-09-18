package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.ScanDocument.scanDocument;

import org.mockito.Mock;
import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.ScanDocumentConverter;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecution.documentqueue.service.SystemIdMapperService;

@ExtendWith(MockitoExtension.class)
public class ScanDocumentConverterTest {

    @InjectMocks
    private ScanDocumentConverter scanDocumentConverter;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Test
    public void shouldConvertToOutstandingDocument() throws Exception {

        final UUID actionedBy = randomUUID();
        final String caseUrn = "case urn";
        final ZonedDateTime deletedDate = new UtcClock().now();
        final ZonedDateTime scanningDate = new UtcClock().now().minusMonths(2);
        final ZonedDateTime statusUpdatedDate = new UtcClock().now().minusHours(3);
        final String casePtiUrn = "case PTI urn";
        final boolean deleted = true;
        final String documentControlNumber = "documentControlNumber";
        final String documentName = "Single Justice Procedure Notice - Plea (Multiple)";
        final String fileName = "file name";
        final String manualIntervention = "manualIntervention";
        final String notes = "notes";
        final String prosecutingAuthorityCode = "prosecuting authority code";
        final String prosecutorAuthorityId = "prosecutorAuthorityId";
        final UUID scanDocumentId = randomUUID();
        final DocumentStatus documentStatus = FOLLOW_UP;
        final String zipFileName ="bae5ffba-80e6-4baa-9a10-8a88460e941f.zip";
        final String envelopeId = "285256e4-23df-475c-87ac-c0dc031349bf";

        final ScanDocument scanDocument = scanDocument()
                .withActionedBy(actionedBy)
                .withCasePTIUrn(casePtiUrn)
                .withCaseUrn(caseUrn)
                .withDeleted(deleted)
                .withDeletedDate(deletedDate)
                .withDocumentControlNumber(documentControlNumber)
                .withDocumentName(documentName)
                .withFileName(fileName)
                .withManualIntervention(manualIntervention)
                .withNotes(notes)
                .withProsecutorAuthorityCode(prosecutingAuthorityCode)
                .withProsecutorAuthorityId(prosecutorAuthorityId)
                .withScanDocumentId(scanDocumentId)
                .withScanningDate(scanningDate)
                .withStatusUpdatedDate(statusUpdatedDate)
                .withStatus(documentStatus)
                .withZipFileName(zipFileName)
                .withEnvelopeId(envelopeId)
                .build();

        final LinkDocumentToCase receiveOutstandingDocument = scanDocumentConverter.asLinkDocumentToCase(scanDocument, fromString(envelopeId));
        final Document outstandingDocument = receiveOutstandingDocument.getDocument();

        assertThat(outstandingDocument.getActionedBy(), is(actionedBy));
        assertThat(outstandingDocument.getCasePTIUrn(), is(casePtiUrn));
        assertThat(outstandingDocument.getCaseUrn(), is(caseUrn));
        assertThat(outstandingDocument.getDocumentControlNumber(), is(documentControlNumber));
        assertThat(outstandingDocument.getFileName(), is(fileName));
        assertThat(outstandingDocument.getManualIntervention(), is(manualIntervention));
        assertThat(outstandingDocument.getNotes(), is(notes));
        assertThat(outstandingDocument.getProsecutorAuthorityCode(), is(prosecutingAuthorityCode));
        assertThat(outstandingDocument.getProsecutorAuthorityId(), is(prosecutorAuthorityId));
        assertThat(outstandingDocument.getScanDocumentId(), is(scanDocumentId));
        assertThat(outstandingDocument.getScanningDate(), is(scanningDate));
        assertThat(outstandingDocument.getStatusUpdatedDate(), is(statusUpdatedDate));
        assertThat(outstandingDocument.getActionedBy(), is(actionedBy));

        assertThat(outstandingDocument.getStatus(), is(not(documentStatus)));
        assertThat(outstandingDocument.getStatus(), is(OUTSTANDING));
        assertThat(outstandingDocument.getSource(), is(Source.BULKSCAN));
        assertThat(outstandingDocument.getZipFileName(), is(zipFileName));
        assertThat(outstandingDocument.getEnvelopeId(), is(envelopeId));
        assertThat(outstandingDocument.getType(), is(Type.PLEA));
    }
}
