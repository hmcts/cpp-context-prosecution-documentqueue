package util;

import java.io.IOException;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class TestUtil {

    public static JsonObject givenPayload(final String filePath) throws IOException {
        try (final InputStream inputStream = TestUtil.class.getResourceAsStream(filePath)) {
            final JsonReader jsonReader = Json.createReader(inputStream);
            return jsonReader.readObject();
        }
    }
}
