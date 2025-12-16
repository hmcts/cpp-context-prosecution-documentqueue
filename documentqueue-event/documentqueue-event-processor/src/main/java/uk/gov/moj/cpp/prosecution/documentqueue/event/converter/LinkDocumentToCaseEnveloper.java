package uk.gov.moj.cpp.prosecution.documentqueue.event.converter;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.json.schemas.stagingbulkscan.FollowUpDocument;
import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeDocument;
import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeRegistered;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentReviewRequired;

import javax.inject.Inject;

public class LinkDocumentToCaseEnveloper {

    private static final String DOCUMENTQUEUE_COMMAND_LINK_OUTSTANDING_DOCUMENT_TO_CASE = "documentqueue.command.link-document-to-case";

    @Inject
    private ScanDocumentConverter scanDocumentConverter;

    @Inject
    private ReviewDocumentConverter reviewDocumentConverter;

    public Envelope<LinkDocumentToCase> toEnvelope(final Envelope<ScanEnvelopeRegistered> registeredScanEnvelope, final ScanDocument scanDocument) {

        final LinkDocumentToCase linkDocumentToCase = scanDocumentConverter
                .asLinkDocumentToCase(scanDocument, registeredScanEnvelope.payload().getScanEnvelope().getScanEnvelopeId());

        return envelop(linkDocumentToCase)
                .withName(DOCUMENTQUEUE_COMMAND_LINK_OUTSTANDING_DOCUMENT_TO_CASE)
                .withMetadataFrom(registeredScanEnvelope);
    }

    public Envelope<LinkDocumentToCase> toEnvelope(final Envelope<FollowUpDocument> followUpDocumentEnvelope, final ScanEnvelopeDocument document) {

        final LinkDocumentToCase linkDocumentToCase = scanDocumentConverter
                .asLinkDocumentToCase(document);

        return envelop(linkDocumentToCase)
                .withName(DOCUMENTQUEUE_COMMAND_LINK_OUTSTANDING_DOCUMENT_TO_CASE)
                .withMetadataFrom(followUpDocumentEnvelope);
    }


    public Envelope<LinkDocumentToCase> toEnvelope(final Envelope<DocumentReviewRequired> documentReviewRequiredEnvelope,
                                                   final String fileName) {
        final LinkDocumentToCase linkDocumentToCase = reviewDocumentConverter
                .asLinkDocumentToCase(documentReviewRequiredEnvelope.payload(), fileName);

        return envelop(linkDocumentToCase)
                .withName(DOCUMENTQUEUE_COMMAND_LINK_OUTSTANDING_DOCUMENT_TO_CASE)
                .withMetadataFrom(documentReviewRequiredEnvelope);
    }

    public Envelope<LinkDocumentToCase> toReviewEnvelope(final Envelope<uk.gov.moj.cps.progression.domain.event.DocumentReviewRequired> documentReviewRequiredEnvelope, final String fileName) {

        final LinkDocumentToCase linkDocumentToCase = reviewDocumentConverter
                .asLinkDocumentToCase(documentReviewRequiredEnvelope.payload(), fileName);

        return envelop(linkDocumentToCase)
                .withName(DOCUMENTQUEUE_COMMAND_LINK_OUTSTANDING_DOCUMENT_TO_CASE)
                .withMetadataFrom(documentReviewRequiredEnvelope);
    }
}
