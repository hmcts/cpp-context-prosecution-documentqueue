package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

import java.util.UUID;
import java.util.function.Supplier;

import javax.inject.Inject;

public class RetrievalServiceProvider {

    @Inject
    private FileServiceRetrievalService fileServiceRetrievalService;

    @Inject
    private MaterialRetrievalService materialRetrievalService;

    public Supplier<String> provide(final UUID fileServiceId, final UUID materialId) {

        if (null != fileServiceId) {
            return () -> fileServiceRetrievalService.retrieveBase64EncodedContent(fileServiceId);
        }

        return () -> materialRetrievalService.retrieveBase64EncodedContent(materialId);
    }
}
