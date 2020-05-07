package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;

import java.util.Objects;
import java.util.UUID;

public class DocumentContent {

    private final UUID documentId;
    private final String fileName;
    private final Status status;
    private final String content;
    private final String mimeType;

    public DocumentContent(
            final UUID documentId,
            final String fileName,
            final Status status,
            final String content, final String mimeType) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.status = status;
        this.content = content;
        this.mimeType = mimeType;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public Status getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentContent)) {
            return false;
        }
        final DocumentContent that = (DocumentContent) o;
        return Objects.equals(documentId, that.documentId) &&
                Objects.equals(fileName, that.fileName) &&
                status == that.status &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, fileName, status, content);
    }

    @Override
    public String toString() {
        return "DocumentContent{" +
                "documentId=" + documentId +
                ", fileName='" + fileName + '\'' +
                ", status=" + status +
                ", content='" + content + '\'' +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }

}
