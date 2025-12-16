package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest;

public enum RestEndpoint {

    GET_DOCUMENT(
            "/documentqueue-query-api/query/api/rest/documentqueue/documents/%s",
            "application/vnd.documentqueue.query.get-document+json"
    ),

    GET_DOCUMENTS(
            "/documentqueue-query-api/query/api/rest/documentqueue/documents",
            "application/vnd.documentqueue.query.documents+json"),

    GET_DOCUMENTS_COUNT(
            "/documentqueue-query-api/query/api/rest/documentqueue/documents/counts",
            "application/vnd.documentqueue.query.documents-counts+json"),

    UPDATE_DOCUMENT_STATUS(
            "/documentqueue-command-api/command/api/rest/documentqueue/documents/%s",
            "application/vnd.documentqueue.update-document-status+json"
    ),

    ATTACH_DOCUMENT(
            "/documentqueue-command-api/command/api/rest/documentqueue/documents/%s",
            "application/vnd.documentqueue.attach-document+json"
    ),

    DELETE_DOCUMENTS_OF_CASES(
            "/documentqueue-command-api/command/api/rest/documentqueue/documents",
                    "application/vnd.documentqueue.delete-documents-of-cases+json"
    ),

    DELETE_EXPIRED_DOCUMENTS(
            "/documentqueue-command-api/command/api/rest/documentqueue/delete/documents",
                    "application/vnd.documentqueue.delete-expired-documents+json"
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
