package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeRegistered;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.OutstandingDocumentEnveloper;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.ScanDocumentConverter;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OutstandingDocumentEnveloperTest {

    @Mock
    private ScanDocumentConverter scanDocumentConverter;

    @InjectMocks
    private OutstandingDocumentEnveloper outstandingDocumentEnveloper;

    @Test
    public void shouldRecreateTheEnvelopeWithTheItsNewName() {

        final ScanEnvelopeRegistered payload = mock(ScanEnvelopeRegistered.class);
        final ScanEnvelope scanEnvelope = mock(ScanEnvelope.class);
        final UUID scanEnvelopeId =  randomUUID();


        final ScanDocument scanDocument = mock(ScanDocument.class);
        final ReceiveOutstandingDocument receiveOutstandingDocument = mock(ReceiveOutstandingDocument.class);
        final Envelope<ScanEnvelopeRegistered> registeredScanEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID())
                        .withName("old.name").build(),
                payload);
        when(payload.getScanEnvelope()).thenReturn(scanEnvelope);
        when(scanEnvelope.getScanEnvelopeId()).thenReturn(scanEnvelopeId);
        when(scanDocumentConverter.asOutstandingDocument(scanDocument, scanEnvelopeId)).thenReturn(receiveOutstandingDocument);

        final Envelope<ReceiveOutstandingDocument> receiveOutstandingDocumentEnvelope = outstandingDocumentEnveloper.toEnvelope(registeredScanEnvelope, scanDocument);

        assertThat(receiveOutstandingDocumentEnvelope.metadata().name(), is("documentqueue.command.receive-outstanding-document"));
        assertThat(receiveOutstandingDocumentEnvelope.payload(), is(receiveOutstandingDocument));
    }
}
