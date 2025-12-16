package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;

import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval.RetrievalServiceProvider;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentContentServiceTest {

    @Mock
    private RetrievalServiceProvider retrievalServiceProvider;

    @InjectMocks
    private DocumentContentService documentContentService;

    @Test
    public void shouldReturnDocumentContent() {

        final UUID documentId = randomUUID();
        final UUID materialId = randomUUID();
        final DocumentContentView documentContentView = mock(DocumentContentView.class);
        final String base64EncodedContent = "base 64 encoded string";
        final String filename = "filename";

        when(documentContentView.getFileServiceId()).thenReturn(null);
        when(documentContentView.getMaterialId()).thenReturn(materialId);
        when(documentContentView.getDocumentId()).thenReturn(documentId);
        when(documentContentView.getFileName()).thenReturn(filename);
        when(documentContentView.getStatus()).thenReturn(IN_PROGRESS);
        when(retrievalServiceProvider.provide(null, materialId)).thenReturn(() -> base64EncodedContent);

        final DocumentContent documentContent = documentContentService.getDocumentContent(documentContentView);

        assertThat(documentContent.getDocumentId(), is(documentId));
        assertThat(documentContent.getFileName(), is(filename));
        assertThat(documentContent.getStatus(), is(IN_PROGRESS));
        assertThat(documentContent.getContent(), is(base64EncodedContent));
    }

    @Test
    public void shoulReturnGetPdfDocument(){
        final String fileName = "My Test File.pdf";
        final ZonedDateTime time = ZonedDateTime.now();
        final ScanDocument scanDocument = ScanDocument
                .scanDocument()
                .withDocumentId(UUID.randomUUID())
                .withFileName(fileName)
                .withReceivedDate(time)
                .build();

        final GetDocument document = documentContentService.getDocument(scanDocument);

        assertThat(document.getDocumentId(), is(scanDocument.getDocumentId()));
        assertThat(document.getFileName(), is(scanDocument.getFileName()));
        assertThat(document.getReceivedDateTime(), is(scanDocument.getReceivedDate()));
        assertThat(document.getMimeType(), is("application/pdf"));
    }
}