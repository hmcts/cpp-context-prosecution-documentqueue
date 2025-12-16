package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

import static java.lang.String.format;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.util.Base64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

public class FileServiceRetrievalService implements RetrievalService {

    @Inject
    private FileRetriever fileRetriever;

    @Inject
    private Base64Encoder base64Encoder;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @SuppressWarnings({"squid:S1166"})
    @Override
    public String retrieveBase64EncodedContent(final UUID fileServiceId) {

        try (final InputStream contentStream = getFileReference(fileServiceId).getContentStream()) {
            return base64Encoder.encode(contentStream);
        } catch (final IOException e) {
            throw new FileRetrievalException(format("Failed to read file with id '%s' from FileService", fileServiceId), e);
        } catch (final FileRetrievalException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    private FileReference getFileReference(final UUID fileServiceId) {
        try {
            return fileRetriever.retrieve(fileServiceId)
                    .orElseThrow(() -> new FileRetrievalException(format("No File found in FileService with id '%s'", fileServiceId)));
        } catch (final FileServiceException e) {
            throw new FileRetrievalException(format("Failed to read file with id '%s' from FileService", fileServiceId), e);
        }
    }
}
