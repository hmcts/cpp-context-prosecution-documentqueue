package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;

import uk.gov.justice.json.schemas.stagingbulkscan.FollowUpDocument;
import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeDocument;
import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeRegistered;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.LinkDocumentToCaseEnveloper;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class BulkScanProcessor {

    @Inject
    private Sender sender;

    @Inject
    private LinkDocumentToCaseEnveloper outstandingDocumentEnveloper;

    @Handles("public.stagingbulkscan.scan-envelope-registered")
    public void processRegisteredScanEnvelope(final Envelope<ScanEnvelopeRegistered> registeredScanEnvelope) {

        registeredScanEnvelope.payload().getScanEnvelope().getAssociatedScanDocuments().stream()
                .filter(scanDocument -> scanDocument.getStatus() == FOLLOW_UP)
                .map(scanDocument -> outstandingDocumentEnveloper.toEnvelope(registeredScanEnvelope, scanDocument))
                .forEach(envelope -> sender.send(envelope));
    }

    @Handles("public.stagingbulkscan.document-marked-for-follow-up")
    public void processDocumentMarkedForFollowUp(final Envelope<FollowUpDocument> followUpDocumentEnvelope) {

        final ScanEnvelopeDocument scanEnvelopeDocument = followUpDocumentEnvelope.payload().getDocument();
        sender.send(outstandingDocumentEnveloper.toEnvelope(followUpDocumentEnvelope, scanEnvelopeDocument));
    }
}
