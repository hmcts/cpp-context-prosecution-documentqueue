package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service;

import uk.gov.justice.prosecution.documentqueue.domain.DocumentContentView;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval.RetrievalServiceProvider;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.tika.Tika;

public class DocumentContentService {

    @Inject
    private RetrievalServiceProvider retrievalServiceProvider;

    public DocumentContent getDocumentContent(final DocumentContentView documentContentView) {

        final UUID fileServiceId = documentContentView.getFileServiceId();
        final UUID materialId = documentContentView.getMaterialId();

        final String content = retrievalServiceProvider.provide(fileServiceId, materialId).get();

        return new DocumentContent(
                documentContentView.getDocumentId(),
                documentContentView.getFileName(),
                documentContentView.getStatus(),
                content,
                mimeType(documentContentView));
    }

    public String mimeType(final DocumentContentView documentContentView) {
        if (documentContentView.getFileName().trim().toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        } else {
            final Tika tika = new Tika();
            return tika.detect(documentContentView.getFileName());
        }
    }
}
