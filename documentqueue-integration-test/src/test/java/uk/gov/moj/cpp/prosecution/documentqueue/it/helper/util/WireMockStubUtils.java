package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.SimpleFileClient.getFileAsString;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.FileUtil.resourceToString;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

public class WireMockStubUtils {

    private static final String CONTENT_TYPE_QUERY_GROUPS = "application/vnd.usersgroups.groups+json";
    private static final String GET_GROUPS_BY_USER_URL = "/usersgroups-service/query/api/rest/usersgroups/users/%s/groups";
    private static final String GET_GROUPS_PAYLOAD_PATH = "json/stub-data/usersgroups.get-groups-by-user.json";
    private static final String SYSTEM_ID_MAPPER_URL = "/system-id-mapper-api/rest/systemid/mappings(.*?)";

    private static final String REFERENCE_DATA_ACTION_SECTION_CODE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/documents-type-access";
    private static final String REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE = "application/vnd.referencedata.query.document-type-access-by-sectioncode+json";

    public static void setupAsAuthorisedUser(final UUID userId, final String userGroup) {
        final Map<String, String> values = new HashMap<>();
        values.put("group", userGroup);

        stubFor(get(urlPathEqualTo(format(GET_GROUPS_BY_USER_URL, userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getFileAsString(GET_GROUPS_PAYLOAD_PATH, values))));

        waitForStubToBeReady(format(GET_GROUPS_BY_USER_URL, userId), CONTENT_TYPE_QUERY_GROUPS);
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType, final Status expectedStatus) {
        poll(requestParams(MessageFormat.format("{0}{1}", getBaseUri(), resource), mediaType).build())
                .until(status().is(expectedStatus));
    }

    public static void stubIdMapperReturningExistingAssociation(final UUID associationId) {
        stubPingFor("system-id-mapper-api");
        stubFor(get(urlPathMatching(SYSTEM_ID_MAPPER_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(systemIdMappingResponseTemplate(associationId))));
        waitForStubToBeReady("/system-id-mapper-api/rest/systemid/mappings/abc", "application/vnd.systemid.mapping+json");

    }

    public static void stubForIdMapperSuccess(final Response.Status status) {
        final String path = "/system-id-mapper-api/rest/systemid(.*?)";
        final String mime = "application/vnd.systemid.map+json";

        stubFor(post(urlPathMatching(path))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(mime))
                .willReturn(aResponse()
                        .withStatus(status.getStatusCode())
                        .withBody(createObjectBuilder().add("id", randomUUID().toString()).build().toString())
                )
        );
    }

    public static void stubForMaterialSuccess(final Response.Status status) {
        stubFor(post(urlPathMatching("/.*/material.*"))
                .willReturn(aResponse()
                        .withStatus(status.getStatusCode())));
    }

    public static void stubForCourtDocumentSuccess(final Response.Status status) {
        stubFor(post(urlPathMatching("/.*/courtdocument.*"))
                .willReturn(aResponse()
                        .withStatus(status.getStatusCode())));
    }

    public static void stubGetReferenceDataBySectionCode( ) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_SECTION_CODE_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE)
                            .withBody(resourceToString("json/stub-data/referencedata.query.document-type-access-by-sectionCode.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_SECTION_CODE_QUERY_URL, REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE);
    }

    private static String systemIdMappingResponseTemplate(final UUID associationId) {

        return "{\n" +
                "  \"mappingId\": \"166c0ae9-e276-4d29-b669-cb32013228b3\",\n" +
                "  \"sourceId\": \"ID01\",\n" +
                "  \"sourceType\": \"SystemACaseId\",\n" +
                "  \"targetId\": \"" + associationId + "\",\n" +
                "  \"targetType\": \"caseId\",\n" +
                "  \"createdAt\": \"2016-09-07T14:30:53.294Z\"\n" +
                "}";
    }

}
