package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest;

public enum RestEndpoint {

    GET_DOCUMENTS(
            "/documentqueue-query-api/query/api/rest/documentqueue/documents",
            "application/vnd.documentqueue.query.documents+json"),

    GET_DOCUMENTS_COUNT(
            "/documentqueue-query-api/query/api/rest/documentqueue/documents/counts",
            "application/vnd.documentqueue.query.documents-counts+json"),

    UPDATE_DOCUMENT_STATUS(
            "/documentqueue-command-api/command/api/rest/documentqueue/documents/%s",
            "application/vnd.documentqueue.update-document-status+json"
    );


    private final String uri;
    private final String mediaType;

    RestEndpoint(final String uri, final String mediaType) {
        this.uri = uri;
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getUri() {
        return uri;
    }
}
