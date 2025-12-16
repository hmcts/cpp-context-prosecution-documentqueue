package uk.gov.moj.cpp.prosecution.documentqueue.event.service;

import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.persistence.DocumentRepository;

import java.util.UUID;

import javax.inject.Inject;

public class DocumentService {

    @Inject
    private DocumentRepository documentRepository;

    public void saveDocument(final Document document) {
        documentRepository.save(document);
    }

    public Document getDocumentByDocumentId(final UUID scanDocumentId) {
        return documentRepository.findBy(scanDocumentId);
    }
}
