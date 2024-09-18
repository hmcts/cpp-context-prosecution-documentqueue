package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.PENDING;

import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeRegistered;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.LinkDocumentToCaseEnveloper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BulkScanProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private LinkDocumentToCaseEnveloper outstandingDocumentEnveloper;

    @InjectMocks
    private BulkScanProcessor bulkScanProcessor;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConvertAllDocumentsToEnvelopesAndSend() throws Exception {

        final Envelope<ScanEnvelopeRegistered> inputEnvelope = mock(Envelope.class);
        final ScanEnvelopeRegistered scanEnvelopeRegistered = mock(ScanEnvelopeRegistered.class);
        final ScanEnvelope scanEnvelope = mock(ScanEnvelope.class);

        final ScanDocument scanDocument_1 = mock(ScanDocument.class);
        final ScanDocument scanDocument_2 = mock(ScanDocument.class);

        final Envelope<LinkDocumentToCase> linkDocumentToCaseEnvelope_1 = mock(Envelope.class);
        final Envelope<LinkDocumentToCase> linkDocumentToCaseEnvelope_2 = mock(Envelope.class);

        when(inputEnvelope.payload()).thenReturn(scanEnvelopeRegistered);
        when(scanEnvelopeRegistered.getScanEnvelope()).thenReturn(scanEnvelope);
        when(scanEnvelope.getAssociatedScanDocuments()).thenReturn(asList(scanDocument_1, scanDocument_2));
        when(scanDocument_1.getStatus()).thenReturn(FOLLOW_UP);
        when(scanDocument_2.getStatus()).thenReturn(FOLLOW_UP);
        when(outstandingDocumentEnveloper.toEnvelope(inputEnvelope, scanDocument_1)).thenReturn(linkDocumentToCaseEnvelope_1);
        when(outstandingDocumentEnveloper.toEnvelope(inputEnvelope, scanDocument_2)).thenReturn(linkDocumentToCaseEnvelope_2);

        bulkScanProcessor.processRegisteredScanEnvelope(inputEnvelope);

        verify(sender).send(linkDocumentToCaseEnvelope_1);
        verify(sender).send(linkDocumentToCaseEnvelope_2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldOnlySendDocumentsOfTypeFollowUp() throws Exception {

        final Envelope<ScanEnvelopeRegistered> inputEnvelope = mock(Envelope.class);
        final ScanEnvelopeRegistered scanEnvelopeRegistered = mock(ScanEnvelopeRegistered.class);
        final ScanEnvelope scanEnvelope = mock(ScanEnvelope.class);

        final ScanDocument scanDocument_1 = mock(ScanDocument.class);
        final ScanDocument scanDocument_2 = mock(ScanDocument.class);
        final ScanDocument scanDocument_3 = mock(ScanDocument.class);
        final ScanDocument scanDocument_4 = mock(ScanDocument.class);

        final Envelope<LinkDocumentToCase> linkDocumentToCaseEnvelope_2 = mock(Envelope.class);

        when(inputEnvelope.payload()).thenReturn(scanEnvelopeRegistered);
        when(scanEnvelopeRegistered.getScanEnvelope()).thenReturn(scanEnvelope);
        when(scanEnvelope.getAssociatedScanDocuments()).thenReturn(asList(
                scanDocument_1,
                scanDocument_2,
                scanDocument_3,
                scanDocument_4
        ));
        when(scanDocument_1.getStatus()).thenReturn(PENDING);
        when(scanDocument_2.getStatus()).thenReturn(FOLLOW_UP);
        when(scanDocument_3.getStatus()).thenReturn(MANUALLY_ACTIONED);
        when(scanDocument_4.getStatus()).thenReturn(AUTO_ACTIONED);
        when(outstandingDocumentEnveloper.toEnvelope(inputEnvelope, scanDocument_2)).thenReturn(linkDocumentToCaseEnvelope_2);

        bulkScanProcessor.processRegisteredScanEnvelope(inputEnvelope);

        verify(sender).send(linkDocumentToCaseEnvelope_2);
    }
}
