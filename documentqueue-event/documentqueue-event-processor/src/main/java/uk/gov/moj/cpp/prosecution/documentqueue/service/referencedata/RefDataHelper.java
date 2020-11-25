package uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecution.documentqueue.json.schemas.DocumentTypeAccessReferenceData;

import java.io.IOException;
import java.util.function.Function;

import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefDataHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(RefDataHelper.class);

    private RefDataHelper() {
    }

    public static Function<JsonValue, DocumentTypeAccessReferenceData> asDocumentsMetadataRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), DocumentTypeAccessReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal DocumentTypeAccessReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

}
