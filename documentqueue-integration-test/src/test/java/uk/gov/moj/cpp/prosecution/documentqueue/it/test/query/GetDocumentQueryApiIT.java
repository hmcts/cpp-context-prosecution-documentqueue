package uk.gov.moj.cpp.prosecution.documentqueue.it.test.query;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newOutstandingDocumentReceived;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.GET_DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.getRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.OUTSTANDING_DOCUMENT_RECEIVED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleResponse;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetDocumentQueryApiIT extends BaseIT {
    private static final UUID DOCUMENT_ID = randomUUID();
    public static final UUID MATERIAL_ID = randomUUID();
    public static final UUID FILE_SERVICE_ID = randomUUID();

    @BeforeEach
    public void setUp() {
        createOutstandingDocuments(newDocumentValues(DOCUMENT_ID, "CPS", "APPLICATION", "OUTSTANDING", MATERIAL_ID, FILE_SERVICE_ID));
    }

    @Test
    public void shouldGetDocument() {
        final UUID userId = UUID.randomUUID();
        setupAsAuthorisedUser(userId, "Crown Court Admin");
        final SimpleResponse response = getRequest(
                format(GET_DOCUMENT.getUri(), DOCUMENT_ID.toString()),
                GET_DOCUMENT.getMediaType(),
                userId,
                OK);
        final JsonObject result = response.asJsonObject();

        assertThat(response.getResponseStatus(), is(200));
        assertThat(result.getString("documentId"), is(DOCUMENT_ID.toString()));
        assertThat(result.getString("fileName"), notNullValue());
        assertThat(result.getString("mimeType"), is("application/pdf"));
        assertThat(result.getString("receivedDateTime"), is("2020-02-12T20:11:32.013Z"));
    }

    @Test
    public void shouldReturn404IfTheDocumentCouldNotFound() {
        final UUID userId = randomUUID();
        final UUID nonExistentDocumentId = randomUUID();
        setupAsAuthorisedUser(userId, "Crown Court Admin");

        final SimpleResponse response = getRequest(
                format(GET_DOCUMENT.getUri(), nonExistentDocumentId.toString()),
                GET_DOCUMENT.getMediaType(),
                userId,
                NOT_FOUND);

        assertThat(response.getResponseStatus(), is(NOT_FOUND.getStatusCode()));
    }

    private void createOutstandingDocuments(Map<String, String> values) {
        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
    }
}
