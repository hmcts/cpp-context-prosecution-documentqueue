package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

import java.util.UUID;

public interface RetrievalService {

    String retrieveBase64EncodedContent(final UUID id);
}
