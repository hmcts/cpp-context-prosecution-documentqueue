package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest;

import java.io.StringReader;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

public class SimpleResponse {

    private int responseStatus;
    private final String body;

    private SimpleResponse(final int responseStatus, final String body) {
        this.responseStatus = responseStatus;
        this.body = body;
    }

    public static SimpleResponse of(final String body) {
        return new SimpleResponse(-1, body);
    }

    public static SimpleResponse of(final int responseStatus, final String body) {
        return new SimpleResponse(responseStatus, body);
    }

    public String asString() {
        return body;
    }

    public JsonObject asJsonObject() {
        return JsonObjects.createReader(new StringReader(body)).readObject();
    }

    public int getResponseStatus() {
        return responseStatus;
    }
}
