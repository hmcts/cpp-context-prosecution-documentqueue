package uk.gov.moj.cpp.prosecution.documentqueue.event.converter;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.json.schemas.stagingbulkscan.FollowUpDocument;
import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeDocument;
import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeRegistered;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentReviewRequired;

import javax.inject.Inject;

public class OutstandingDocumentEnveloper {

    private static final String DOCUMENTQUEUE_COMMAND_RECEIVE_OUTSTANDING_DOCUMENT = "documentqueue.command.receive-outstanding-document";

    @Inject
    private ScanDocumentConverter scanDocumentConverter;

    @Inject
    private ReviewDocumentConverter reviewDocumentConverter;

    public Envelope<ReceiveOutstandingDocument> toEnvelope(final Envelope<ScanEnvelopeRegistered> registeredScanEnvelope, final ScanDocument scanDocument) {

        final ReceiveOutstandingDocument receiveOutstandingDocument = scanDocumentConverter
                .asOutstandingDocument(scanDocument, registeredScanEnvelope.payload().getScanEnvelope().getScanEnvelopeId());

        return envelop(receiveOutstandingDocument)
                .withName(DOCUMENTQUEUE_COMMAND_RECEIVE_OUTSTANDING_DOCUMENT)
                .withMetadataFrom(registeredScanEnvelope);
    }

    public Envelope<ReceiveOutstandingDocument> toEnvelope(final Envelope<FollowUpDocument> followUpDocumentEnvelope, final ScanEnvelopeDocument document) {

        final ReceiveOutstandingDocument receiveOutstandingDocument = scanDocumentConverter
                .asOutstandingDocument(document);

        return envelop(receiveOutstandingDocument)
                .withName(DOCUMENTQUEUE_COMMAND_RECEIVE_OUTSTANDING_DOCUMENT)
                .withMetadataFrom(followUpDocumentEnvelope);
    }


    public Envelope<ReceiveOutstandingDocument> toEnvelope(final Envelope<DocumentReviewRequired> documentReviewRequiredEnvelope, final String fileName) {
        final ReceiveOutstandingDocument receiveOutstandingDocument = reviewDocumentConverter
                .asOutstandingDocument(documentReviewRequiredEnvelope.payload(), fileName);
        return envelop(receiveOutstandingDocument)
                .withName(DOCUMENTQUEUE_COMMAND_RECEIVE_OUTSTANDING_DOCUMENT)
                .withMetadataFrom(documentReviewRequiredEnvelope);
    }

    public Envelope<ReceiveOutstandingDocument> toReviewEnvelope(final Envelope<uk.gov.moj.cps.progression.domain.event.DocumentReviewRequired> documentReviewRequiredEnvelope, final String fileName) {

        final ReceiveOutstandingDocument receiveOutstandingDocument = reviewDocumentConverter
                .asOutstandingDocument(documentReviewRequiredEnvelope.payload(), fileName);

        return envelop(receiveOutstandingDocument)
                .withName(DOCUMENTQUEUE_COMMAND_RECEIVE_OUTSTANDING_DOCUMENT)
                .withMetadataFrom(documentReviewRequiredEnvelope);
    }
}
