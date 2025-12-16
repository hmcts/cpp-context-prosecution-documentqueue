package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

public class MaterialRetrievalException extends RuntimeException {

    public MaterialRetrievalException(final String message) {
        super(message);
    }

    public MaterialRetrievalException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
