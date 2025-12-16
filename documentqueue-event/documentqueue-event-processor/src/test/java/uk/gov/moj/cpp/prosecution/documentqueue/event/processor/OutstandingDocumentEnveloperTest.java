package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeRegistered;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.LinkDocumentToCaseEnveloper;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.ScanDocumentConverter;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OutstandingDocumentEnveloperTest {

    @Mock
    private ScanDocumentConverter scanDocumentConverter;

    @InjectMocks
    private LinkDocumentToCaseEnveloper outstandingDocumentEnveloper;

    @Test
    public void shouldRecreateTheEnvelopeWithTheItsNewName() {

        final ScanEnvelopeRegistered payload = mock(ScanEnvelopeRegistered.class);
        final ScanEnvelope scanEnvelope = mock(ScanEnvelope.class);
        final UUID scanEnvelopeId =  randomUUID();


        final ScanDocument scanDocument = mock(ScanDocument.class);
        final LinkDocumentToCase linkDocumentToCase = mock(LinkDocumentToCase.class);
        final Envelope<ScanEnvelopeRegistered> registeredScanEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID())
                        .withName("old.name").build(),
                payload);
        when(payload.getScanEnvelope()).thenReturn(scanEnvelope);
        when(scanEnvelope.getScanEnvelopeId()).thenReturn(scanEnvelopeId);
        when(scanDocumentConverter.asLinkDocumentToCase(scanDocument, scanEnvelopeId)).thenReturn(linkDocumentToCase);

        final Envelope<LinkDocumentToCase> linkDocumentToCaseEnvelope = outstandingDocumentEnveloper.toEnvelope(registeredScanEnvelope, scanDocument);

        assertThat(linkDocumentToCaseEnvelope.metadata().name(), is("documentqueue.command.link-document-to-case"));
        assertThat(linkDocumentToCaseEnvelope.payload(), is(linkDocumentToCase));
    }
}
