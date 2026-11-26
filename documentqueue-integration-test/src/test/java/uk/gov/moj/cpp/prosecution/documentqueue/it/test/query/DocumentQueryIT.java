package uk.gov.moj.cpp.prosecution.documentqueue.it.test.query;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonObject;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newOutstandingDocumentReceived;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.GET_DOCUMENTS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.GET_DOCUMENTS_COUNT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.getRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.OUTSTANDING_DOCUMENT_RECEIVED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleResponse;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DocumentQueryIT extends BaseIT {

    @BeforeEach
    public void setUp() {
        final Map<String, String> values1 = newDocumentValues("BULKSCAN", "PLEA", "OUTSTANDING");
        final Map<String, String> values2 = newDocumentValues("CPS", "APPLICATION", "OUTSTANDING");
        final Map<String, String> values3 = newDocumentValues("OTHER", "CORRESPONDENCE", "OUTSTANDING");
        final Map<String, String> values4 = newDocumentValues("BULKSCAN", "CORRESPONDENCE", "OUTSTANDING");
        final Map<String, String> values5 = newDocumentValues("OTHER", "APPLICATION", "OUTSTANDING");
        final Map<String, String> values6 = newDocumentValues("CPS", "PLEA", "OUTSTANDING");
        createOutstandingDocuments(values1);
        createOutstandingDocuments(values2);
        createOutstandingDocuments(values3);
        createOutstandingDocuments(values4);
        createOutstandingDocuments(values5);
        createOutstandingDocuments(values6);
    }

    @Test
    public void testQueryForAllDocuments() throws IOException { //4
        final UUID userId = UUID.randomUUID();
        setupAsAuthorisedUser(userId, "Crown Court Admin");
        final SimpleResponse result = getRequest(GET_DOCUMENTS, userId.toString());
        final JsonObject responsePayload = result.asJsonObject();
        final Optional<JsonArray> documents = getJsonArray(responsePayload, "documents");
        assertThat(documents.isPresent(), is(true));
        assertThat(documents.get().size(), is(6));
    }

    @Test
    public void testQueryForDocumentsWithSource() { //1
        final UUID userId = UUID.randomUUID();
        setupAsAuthorisedUser(userId, "Listing Officers");
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("source", "BULKSCAN");
        final SimpleResponse result = getRequest(GET_DOCUMENTS, queryParams, userId.toString());
        final JsonObject responsePayload = result.asJsonObject();
        final Optional<JsonArray> documents = getJsonArray(responsePayload, "documents");
        assertThat(documents.isPresent(), is(true));
        assertThat(documents.get().size(), is(2));
    }

    @Test
    public void testQueryForDocumentsWithStatus() { //2
        final UUID userId = UUID.randomUUID();
        setupAsAuthorisedUser(userId, "Court Clerks");
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("status", "OUTSTANDING");
        final SimpleResponse result = getRequest(GET_DOCUMENTS, queryParams, userId.toString());
        final JsonObject responsePayload = result.asJsonObject();
        final Optional<JsonArray> documents = getJsonArray(responsePayload, "documents");
        assertThat(documents.isPresent(), is(true));
        assertThat(documents.get().size(), is(6));
    }

    @Test
    public void testQueryForDocumentsWithSourceAndStatus() { //3
        final UUID userId = UUID.randomUUID();
        setupAsAuthorisedUser(userId, "Legal Advisers");
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("status", "OUTSTANDING");
        queryParams.put("source", "BULKSCAN");
        final SimpleResponse result = getRequest(GET_DOCUMENTS, queryParams, userId.toString());
        final JsonObject responsePayload = result.asJsonObject();
        final Optional<JsonArray> documents = getJsonArray(responsePayload, "documents");
        assertThat(documents.isPresent(), is(true));
        assertThat(documents.get().size(), is(2));
    }

    @Test
    public void testQueryForDocumentsCount() { //5
        final UUID userId = UUID.randomUUID();
        setupAsAuthorisedUser(userId, "Legal Advisers");
        final SimpleResponse result = getRequest(GET_DOCUMENTS_COUNT, userId.toString());
        final JsonObject responsePayload = result.asJsonObject();
        final Optional<JsonObject> completed = getJsonObject(responsePayload, "completed");
        final Optional<JsonObject> inProgress = getJsonObject(responsePayload, "inProgress");
        final Optional<JsonObject> outstanding = getJsonObject(responsePayload, "outstanding");
        assertThat(completed.isPresent(), is(true));
        assertThat(inProgress.isPresent(), is(true));
        assertThat(outstanding.isPresent(), is(true));
    }

    private void createOutstandingDocuments(Map<String, String> values) {
        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
    }

}
