package uk.gov.moj.cpp.prosecution.documentqueue.it.test.query;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newOutstandingDocumentReceived;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.getRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.OUTSTANDING_DOCUMENT_RECEIVED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.FileServiceInserter;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleResponse;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.util.Base64Encoder;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;


public class FileServiceDocumentQueryApiIT extends BaseIT {

    private static final String QUERY_API_GET_DOCUMENT_URI = "/documentqueue-query-api/query/api/rest/documentqueue/documents/%s";
    private static final String QUERY_API_GET_DOCUMENT_METADATA = "application/vnd.documentqueue.query.document-content+json";
    private static final UUID DOCUMENT_ID = randomUUID();
    public static final UUID MATERIAL_ID = randomUUID();
    public static final UUID FILE_SERVICE_ID = randomUUID();
    private static final String FILE_NAME = "XVBN22.pdf";
    private static final String PATH_ON_CLASSPATH = "/materials";

    private final FileServiceInserter fileServiceInserter = new FileServiceInserter();
    private final Base64Encoder base64Encoder = new Base64Encoder();

    @BeforeEach
    public void setUp() {
        createOutstandingDocuments(newDocumentValues("BULKSCAN", "PLEA", "OUTSTANDING"));
        createOutstandingDocuments(newDocumentValues(DOCUMENT_ID, "CPS", "APPLICATION", "OUTSTANDING", MATERIAL_ID, FILE_SERVICE_ID));
        createOutstandingDocuments(newDocumentValues("OTHER", "CORRESPONDENCE", "OUTSTANDING"));
    }

    @BeforeEach
    public void insertPdfIntoFileServiceDatabase() throws Exception {

        fileServiceInserter.clean();
        fileServiceInserter.addPdf(FILE_SERVICE_ID, PATH_ON_CLASSPATH, FILE_NAME);
    }

    @Test
    public void shouldReturnSingleDocumentContentInformationFromQueryView() throws Exception {

        final UUID userId = randomUUID();
        setupAsAuthorisedUser(userId, "Crown Court Admin");

        final SimpleResponse result = getRequest(
                format(QUERY_API_GET_DOCUMENT_URI, DOCUMENT_ID.toString()),
                QUERY_API_GET_DOCUMENT_METADATA,
                userId,
                OK);

        final JsonObject responsePayload = result.asJsonObject();

        assertThat(responsePayload.getString("documentId"), is(DOCUMENT_ID.toString()));
        assertThat(responsePayload.getString("status"), is(OUTSTANDING.toString()));
        assertThat(responsePayload.getString("fileName"), is("financial_means_with_ocr_data.pdf"));
        final String content = responsePayload.getString("content");

        final InputStream inputStream = getClass()
                .getResourceAsStream(PATH_ON_CLASSPATH + "/" + FILE_NAME);

        assertThat(content, is(base64Encoder.encode(inputStream)));
    }

    @Test
    public void shouldReturn404IfTheFileIsNotFound() throws Exception {

        final UUID userId = randomUUID();
        final UUID nonExistentDocumentId = randomUUID();
        setupAsAuthorisedUser(userId, "Crown Court Admin");

        final SimpleResponse result = getRequest(
                format(QUERY_API_GET_DOCUMENT_URI, nonExistentDocumentId),
                QUERY_API_GET_DOCUMENT_METADATA,
                userId,
                NOT_FOUND);

        assertThat(result.getResponseStatus(), is(NOT_FOUND.getStatusCode()));
    }

    private void createOutstandingDocuments(Map<String, String> values) {

        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
    }
}
