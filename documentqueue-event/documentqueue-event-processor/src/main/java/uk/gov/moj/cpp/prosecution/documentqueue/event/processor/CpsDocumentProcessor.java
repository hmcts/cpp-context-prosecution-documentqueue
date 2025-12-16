package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.LinkDocumentToCaseEnveloper;
import uk.gov.moj.cpp.prosecution.documentqueue.service.material.MaterialService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentReviewRequired;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class CpsDocumentProcessor {

    @Inject
    FileService fileService;

    @Inject
    Sender sender;

    @Inject
    private LinkDocumentToCaseEnveloper linkDocumentToCaseEnveloper;

    @Inject
    private MaterialService materialService;

    @SuppressWarnings("squid:S3655")
    @Handles("public.prosecutioncasefile.document-review-required")
    public void processDocumentReviewRequiredEnvelope(final Envelope<DocumentReviewRequired> documentReviewRequiredEnvelope) throws FileServiceException {
        final Optional<JsonObject> fileMetadata = fileService.retrieveMetadata(documentReviewRequiredEnvelope.payload().getFileStoreId());

        final String fileName = fileMetadata.get().getString("fileName");
        final Envelope<LinkDocumentToCase> envelope = linkDocumentToCaseEnveloper.toEnvelope(documentReviewRequiredEnvelope, fileName);
        sender.send(envelope);
    }

    @Handles("public.progression.document-review-required")
    public void processDocumentReviewRequired(final Envelope<uk.gov.moj.cps.progression.domain.event.DocumentReviewRequired> documentReviewRequiredEnvelope) {
        final String fileName = materialService.materialMetaDataForMaterialId(documentReviewRequiredEnvelope.payload().getMaterialId()).getFileName();
        final Envelope<LinkDocumentToCase> envelope = linkDocumentToCaseEnveloper.toReviewEnvelope(documentReviewRequiredEnvelope, fileName);
        sender.send(envelope);
    }
}
