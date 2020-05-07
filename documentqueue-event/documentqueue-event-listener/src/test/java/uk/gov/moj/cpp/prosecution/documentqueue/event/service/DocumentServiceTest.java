package uk.gov.moj.cpp.prosecution.documentqueue.event.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.persistence.DocumentRepository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    public void shouldSaveADocument() throws Exception {

        final Document document = mock(Document.class);

        documentService.saveDocument(document);

        verify(documentRepository).save(document);
    }

    @Test
    public void shouldFindDocumentById() throws Exception {

        final UUID documentId = randomUUID();
        final Document document = mock(Document.class);

        when(documentRepository.findBy(documentId)).thenReturn(document);

        assertThat(documentService.getDocumentByDocumentId(documentId), is(document));
    }
}
