package uk.gov.moj.cpp.prosecution.documentqueue.it.test.query;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newOutstandingDocumentReceived;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.GET_DOCUMENTS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.GET_DOCUMENTS_COUNT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.getRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.OUTSTANDING_DOCUMENT_RECEIVED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master) against the deployed service.
 *
 * DQ-I01 (BC-08, DQ-P04 / DQ-P08): the documents query returns ScanDocument values read back from PostgreSQL through
 * the rewritten DocumentQueueRepository; pin each returned document in full, including the timestamps.
 * DQ-I02 (DQ-P06): the documents-counts query, built from the rewritten getDocumentCount JPQL.
 * DQ-I03 (BC-16): the framework metrics endpoints under /internal/metrics.
 *
 * BaseIT cleans the viewstore before each test and the event template is fixed, so with fixed ids both responses
 * are deterministic.
 */
public class JavaUpgradeParityIT extends BaseIT {

    private static final UUID BULKSCAN_PLEA = fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID CPS_APPLICATION = fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID OTHER_CORRESPONDENCE = fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID CPS_PLEA_COMPLETED = fromString("44444444-4444-4444-8444-444444444444");

    private static final String METRICS_PING_URL = getBaseUri() + "/documentqueue-service/internal/metrics/ping";

    @BeforeEach
    public void createDocuments() {
        create(BULKSCAN_PLEA, "BULKSCAN", "PLEA", "OUTSTANDING");
        create(CPS_APPLICATION, "CPS", "APPLICATION", "OUTSTANDING");
        create(OTHER_CORRESPONDENCE, "OTHER", "CORRESPONDENCE", "OUTSTANDING");
        create(CPS_PLEA_COMPLETED, "CPS", "PLEA", "COMPLETED");
    }

    @Test
    public void dqI01_documentsQueryShouldReturnTheJ17Documents() {
        final UUID userId = UUID.randomUUID();
        setupAsAuthorisedUser(userId, "Crown Court Admin");

        final JsonObject response = getRequest(GET_DOCUMENTS, userId.toString()).asJsonObject();

        final Map<String, String> documentsById = new TreeMap<>();
        response.getJsonArray("documents").getValuesAs(JsonObject.class)
                .forEach(document -> documentsById.put(document.getString("documentId"), document.toString()));
        assertThat(documentsById.toString(), is("{11111111-1111-4111-8111-111111111111={\"availableAsPdf\":true,\"caseUrn\":\"TVL6794003OMW\",\"documentId\":\"11111111-1111-4111-8111-111111111111\",\"documentName\":\"SJPMC100\",\"documentType\":\"PLEA\",\"envelopeId\":\"285256e4-23df-475c-87ac-c0dc031349bf\",\"fileName\":\"financial_means_with_ocr_data.pdf\",\"prosecutorAuthorityCode\":\"TVL\",\"receivedDate\":\"2020-02-12T20:11:32.013Z\",\"source\":\"BULKSCAN\",\"status\":\"OUTSTANDING\",\"statusUpdatedDate\":\"2020-02-12T20:51:32.013Z\"}, "
                + "22222222-2222-4222-8222-222222222222={\"availableAsPdf\":true,\"caseUrn\":\"TVL6794003OMW\",\"documentId\":\"22222222-2222-4222-8222-222222222222\",\"documentName\":\"SJPMC100\",\"documentType\":\"APPLICATION\",\"envelopeId\":\"285256e4-23df-475c-87ac-c0dc031349bf\",\"fileName\":\"financial_means_with_ocr_data.pdf\",\"prosecutorAuthorityCode\":\"TVL\",\"receivedDate\":\"2020-02-12T20:11:32.013Z\",\"source\":\"CPS\",\"status\":\"OUTSTANDING\",\"statusUpdatedDate\":\"2020-02-12T20:51:32.013Z\"}, "
                + "33333333-3333-4333-8333-333333333333={\"availableAsPdf\":true,\"caseUrn\":\"TVL6794003OMW\",\"documentId\":\"33333333-3333-4333-8333-333333333333\",\"documentName\":\"SJPMC100\",\"documentType\":\"CORRESPONDENCE\",\"envelopeId\":\"285256e4-23df-475c-87ac-c0dc031349bf\",\"fileName\":\"financial_means_with_ocr_data.pdf\",\"prosecutorAuthorityCode\":\"TVL\",\"receivedDate\":\"2020-02-12T20:11:32.013Z\",\"source\":\"OTHER\",\"status\":\"OUTSTANDING\",\"statusUpdatedDate\":\"2020-02-12T20:51:32.013Z\"}, "
                + "44444444-4444-4444-8444-444444444444={\"availableAsPdf\":true,\"caseUrn\":\"TVL6794003OMW\",\"documentId\":\"44444444-4444-4444-8444-444444444444\",\"documentName\":\"SJPMC100\",\"documentType\":\"PLEA\",\"envelopeId\":\"285256e4-23df-475c-87ac-c0dc031349bf\",\"fileName\":\"financial_means_with_ocr_data.pdf\",\"prosecutorAuthorityCode\":\"TVL\",\"receivedDate\":\"2020-02-12T20:11:32.013Z\",\"source\":\"CPS\",\"status\":\"COMPLETED\",\"statusUpdatedDate\":\"2020-02-12T20:51:32.013Z\"}}"));
        assertThat(withoutDocuments(response), is("{page=1, pageSize=50, sortBy=\"vendorReceivedDate\", total=4}"));
    }

    @Test
    public void dqI02_documentsCountQueryShouldReturnTheJ17Counts() {
        final UUID userId = UUID.randomUUID();
        setupAsAuthorisedUser(userId, "Legal Advisers");

        assertThat(getRequest(GET_DOCUMENTS_COUNT, userId.toString()).asJsonObject().toString(), is("{\"completed\":{\"applications\":{\"bulkScan\":0,\"volume\":0},\"correspondence\":{\"bulkScan\":0,\"volume\":0},\"other\":{\"bulkScan\":0,\"volume\":0},\"pleas\":{\"bulkScan\":0,\"volume\":1},\"total\":{\"bulkScan\":0,\"volume\":1},\"totalCount\":1},"
                + "\"inProgress\":{\"applications\":{\"bulkScan\":0,\"volume\":0},\"correspondence\":{\"bulkScan\":0,\"volume\":0},\"other\":{\"bulkScan\":0,\"volume\":0},\"pleas\":{\"bulkScan\":0,\"volume\":0},\"total\":{\"bulkScan\":0,\"volume\":0},\"totalCount\":0},"
                + "\"outstanding\":{\"applications\":{\"bulkScan\":0,\"volume\":1},\"correspondence\":{\"bulkScan\":0,\"volume\":1},\"other\":{\"bulkScan\":0,\"volume\":0},\"pleas\":{\"bulkScan\":1,\"volume\":1},\"total\":{\"bulkScan\":1,\"volume\":3},\"totalCount\":3}}"));
    }

    @Test
    public void dqI03_metricsPingShouldAnswerWithTheJ17Status() throws IOException, InterruptedException {
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(METRICS_PING_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(200));
    }

    private static String withoutDocuments(final JsonObject response) {
        final Map<String, JsonValue> rest = new TreeMap<>(response);
        rest.remove("documents");
        return rest.toString();
    }

    private static void create(final UUID documentId, final String source, final String type, final String status) {
        // material and file-service ids derived from the document id, so they are fixed too
        final Map<String, String> values = newDocumentValues(documentId, source, type, status,
                fromString(documentId.toString().replaceFirst("^.", "a")),
                fromString(documentId.toString().replaceFirst("^.", "b")));
        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
    }
}
