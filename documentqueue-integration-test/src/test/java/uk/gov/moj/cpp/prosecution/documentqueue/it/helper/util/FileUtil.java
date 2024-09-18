package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static String resourceToString(final String path, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(path)) {
            return format(IOUtils.toString(systemResourceAsStream), placeholders);
        } catch (final IOException e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
            throw new UncheckedIOException(e);
        }
    }

}
