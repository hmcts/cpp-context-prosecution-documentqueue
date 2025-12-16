package uk.gov.moj.cpp.prosecution.documentqueue.query.api.util;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

public class Base64EncoderTest {

    private final Base64Encoder base64Encoder = new Base64Encoder();

    @Test
    public void shouldBase64EncodeAnInputStream() throws Exception {
        
        try(final InputStream inputStream = toInputStream("There is no spoon")) {
            assertThat(base64Encoder.encode(inputStream), is("VGhlcmUgaXMgbm8gc3Bvb24="));
        }
    }

    @Test
    public void shouldFailIfTheInputStreamCannotBeRead() throws Exception {

        final IOException ioException = new IOException("Ooops");

        final InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any())).thenThrow(ioException);

        try {
            base64Encoder.encode(inputStream);
            fail();
        } catch (final Base64EncodingException expected) {
            assertThat(expected.getMessage(), is("Failed to encode InputStream to base64"));
            assertThat(expected.getCause(), is(ioException));
        }
    }
}
