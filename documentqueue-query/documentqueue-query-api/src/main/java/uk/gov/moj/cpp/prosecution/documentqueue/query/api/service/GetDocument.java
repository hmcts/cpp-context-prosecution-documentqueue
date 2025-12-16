package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class GetDocument {

    private final UUID documentId;
    private final String fileName;
    private final ZonedDateTime receivedDateTime;
    private final String mimeType;

    public GetDocument(
            final UUID documentId,
            final String fileName,
            final ZonedDateTime receivedDateTime,
            final String mimeType) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.receivedDateTime = receivedDateTime;
        this.mimeType = mimeType;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public ZonedDateTime getReceivedDateTime() {
        return receivedDateTime;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GetDocument that = (GetDocument) o;
        return Objects.equals(documentId, that.documentId) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(receivedDateTime, that.receivedDateTime) &&
                Objects.equals(mimeType, that.mimeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, fileName, receivedDateTime, mimeType);
    }

    @Override
    public String toString() {
        return "GetDocument{" +
                "documentId=" + documentId +
                ", fileName='" + fileName + '\'' +
                ", receivedDateTime=" + receivedDateTime +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
}
