package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;

import uk.gov.justice.prosecution.documentqueue.domain.DocumentContentView;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval.RetrievalServiceProvider;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
}