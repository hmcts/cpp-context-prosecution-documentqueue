package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.SimpleFileClient.getFileAsString;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

public class WireMockStubUtils {

    private static final String CONTENT_TYPE_QUERY_GROUPS = "application/vnd.usersgroups.groups+json";
    private static final String GET_GROUPS_BY_USER_URL = "/usersgroups-service/query/api/rest/usersgroups/users/%s/groups";
    private static final String GET_GROUPS_PAYLOAD_PATH = "json/stub-data/usersgroups.get-groups-by-user.json";

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

}
