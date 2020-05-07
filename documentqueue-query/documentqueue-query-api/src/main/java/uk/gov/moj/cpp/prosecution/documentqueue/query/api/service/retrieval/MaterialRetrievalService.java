package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

import static java.lang.String.format;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.util.Base64Encoder;

import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;

public class MaterialRetrievalService implements RetrievalService {

    @Inject
    private MaterialClient materialClient;

    @Inject
    private Base64Encoder base64Encoder;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Override
    @SuppressWarnings({"squid:S2221", "squid:S3655"})
    public String retrieveBase64EncodedContent(final UUID materialId) {
        final UUID systemUserId = systemUserProvider.getContextSystemUserId().get();
        try (final InputStream materialResponse = materialClient.getMaterial(materialId, systemUserId).readEntity(InputStream.class)) {
            return base64Encoder.encode(materialResponse);
        } catch (Exception e) {
            throw new MaterialRetrievalException(format("Failed to retrieve material by id %s", materialId), e);
        }
    }
}
