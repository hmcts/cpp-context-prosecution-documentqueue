package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

public class FileRetrievalException extends RuntimeException {

    public FileRetrievalException(final String message) {
        super(message);
    }

    public FileRetrievalException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
