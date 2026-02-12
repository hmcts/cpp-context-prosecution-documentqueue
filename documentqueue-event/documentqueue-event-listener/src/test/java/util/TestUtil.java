package util;

import java.io.IOException;
import java.io.InputStream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class TestUtil {

    public static JsonObject givenPayload(final String filePath) throws IOException {
        try (final InputStream inputStream = TestUtil.class.getResourceAsStream(filePath)) {
            final JsonReader jsonReader = JsonObjects.createReader(inputStream);
            return jsonReader.readObject();
        }
    }
}
