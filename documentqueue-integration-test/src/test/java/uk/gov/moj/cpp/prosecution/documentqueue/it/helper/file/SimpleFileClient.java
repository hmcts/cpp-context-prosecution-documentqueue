package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file;

import static java.util.Collections.emptyMap;

import uk.gov.justice.services.test.utils.core.files.ClasspathFileResource;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleFileClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFileClient.class);

    public static SimpleResponse getFile(final String path) {
        return getFile(path, emptyMap());
    }

    public static SimpleResponse getFile(final String path, final Map<String, String> valuesMap) {
        try {
            final String string = Resources.toString(Resources.getResource(path), Charset.defaultCharset());

            return SimpleResponse.of(new StrSubstitutor(valuesMap).replace(string));
        } catch (IOException exception) {
            LOGGER.error(String.format("IOException while reading the file from path %s", path), exception);
        }

        return SimpleResponse.of("");
    }

    public static String getFileAsString(final String path, final Map<String, String> valuesMap)  {
        try {
            final String string = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
            return new StrSubstitutor(valuesMap).replace(string);
        } catch (IOException exception) {
            LOGGER.error(String.format("IOException while reading the file from path %s", path), exception);
        }
        return "";
    }

    public static byte[] getFileAsBytes(final String path) throws IOException {
        final File file = new ClasspathFileResource().getFileFromClasspath(path);
        return FileUtils.readFileToByteArray(file);
    }

    public static String getFileAsString(final String path) throws IOException {
        final File file = new ClasspathFileResource().getFileFromClasspath(path);
        return FileUtils.readFileToString(file);
    }
}
