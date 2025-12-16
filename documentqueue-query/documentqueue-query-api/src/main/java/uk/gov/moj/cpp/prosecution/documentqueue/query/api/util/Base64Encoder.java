package uk.gov.moj.cpp.prosecution.documentqueue.query.api.util;

import static java.util.Base64.getEncoder;
import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.IOException;
import java.io.InputStream;

public class Base64Encoder {

    public String encode(final InputStream inputStream) {

        try {
            final byte[] bytes = toByteArray(inputStream);
            return new String(getEncoder().encode(bytes));
        } catch (final IOException e) {
           throw new Base64EncodingException("Failed to encode InputStream to base64", e);
        }
    }
}
