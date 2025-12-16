package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.util.Base64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class FileServiceRetrievalServiceTest {

    @Mock
    private FileRetriever fileRetriever;

    @Mock
    private Base64Encoder base64Encoder;

    @Mock
    private Logger logger;

    @InjectMocks
    private FileServiceRetrievalService fileServiceRetrievalService;

    @Test
    public void shouldGetTheFileFromTheFileServiceAndBase64Encoded() throws Exception {

        final UUID fileServiceId = randomUUID();
        final String base64EncodedFile = "some encoded bytes";
        final FileReference fileReference = mock(FileReference.class);
        final InputStream contentStream = mock(InputStream.class);

        when(fileRetriever.retrieve(fileServiceId)).thenReturn(of(fileReference));
        when(fileReference.getContentStream()).thenReturn(contentStream);
        when(base64Encoder.encode(contentStream)).thenReturn(base64EncodedFile);

        assertThat(fileServiceRetrievalService.retrieveBase64EncodedContent(fileServiceId), is(base64EncodedFile));

        verify(contentStream).close();
    }

    @Test
    public void shouldReturnNullIfTheFileIsNotFound() throws Exception {

        final UUID fileServiceId = fromString("b758d9c8-a1ab-4d58-a2c7-66a90a5ddda0");

        when(fileRetriever.retrieve(fileServiceId)).thenReturn(empty());

        assertThat(fileServiceRetrievalService.retrieveBase64EncodedContent(fileServiceId), nullValue());
        verify(logger).error("No File found in FileService with id 'b758d9c8-a1ab-4d58-a2c7-66a90a5ddda0'");
    }

    @Test
    public void shouldReturnNullIfRetrievingTheFileFails() throws Exception {

        final FileServiceException fileServiceException = new FileServiceException("Ooops");

        final UUID fileServiceId = fromString("b758d9c8-a1ab-4d58-a2c7-66a90a5ddda0");

        when(fileRetriever.retrieve(fileServiceId)).thenThrow(fileServiceException);

        assertThat(fileServiceRetrievalService.retrieveBase64EncodedContent(fileServiceId), nullValue());
        verify(logger).error("Failed to read file with id 'b758d9c8-a1ab-4d58-a2c7-66a90a5ddda0' from FileService");
    }

    @Test
    public void shouldFailIfBase64EncodingTheFileContentsFails() throws Exception {

        final IOException ioException = new IOException("Ooops");

        final UUID fileServiceId = fromString("b758d9c8-a1ab-4d58-a2c7-66a90a5ddda0");
        final String base64EncodedFile = "some encoded bytes";

        final FileReference fileReference = mock(FileReference.class);
        final InputStream contentStream = mock(InputStream.class);

        when(fileRetriever.retrieve(fileServiceId)).thenReturn(of(fileReference));
        when(fileReference.getContentStream()).thenReturn(contentStream);
        when(base64Encoder.encode(contentStream)).thenReturn(base64EncodedFile);
        doThrow(ioException).when(contentStream).close();

        try {
            fileServiceRetrievalService.retrieveBase64EncodedContent(fileServiceId);
            fail();
        } catch (final FileRetrievalException expected) {
            assertThat(expected.getCause(), is(ioException));
            assertThat(expected.getMessage(), is("Failed to read file with id 'b758d9c8-a1ab-4d58-a2c7-66a90a5ddda0' from FileService"));
        }

        verify(contentStream).close();
    }
}
