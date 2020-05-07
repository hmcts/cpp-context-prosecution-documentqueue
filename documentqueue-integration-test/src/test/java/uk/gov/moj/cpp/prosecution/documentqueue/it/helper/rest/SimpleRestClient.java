package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.PollingRequestParamsBuilder.pollingRequestParams;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.common.host.TestHostProvider;
import uk.gov.justice.services.test.utils.core.http.PollingRequestParams;
import uk.gov.justice.services.test.utils.core.http.PollingRestClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.collect.Maps;

public class SimpleRestClient {

    public static SimpleResponse getRequest(final RestEndpoint restEndpoint, final String userId) {
        return getRequest(restEndpoint, Maps.newHashMap(), userId.toString(), Response.Status.OK);
    }

    public static SimpleResponse getSubscriptionsFor(final String topicName) {
        final String url = "http://" + TestHostProvider.getHost() + ":8161/jolokia/exec/org.apache.activemq.artemis:type=Broker,brokerName=%22default%22,module=JMS,serviceType=Topic,name=%22" + topicName + "%22/listAllSubscriptions()";
        final Response response = new RestClient().query(url, "text/plain");
        final int responseStatus = response.getStatus();

        assertThat(responseStatus, equalTo(Response.Status.OK.getStatusCode()));

        return SimpleResponse.of(responseStatus, response.readEntity(String.class));
    }

    public static SimpleResponse getRequest(final RestEndpoint restEndpoint, final Map<String, String> queryParams, final String userId) {
        return getRequest(restEndpoint, queryParams, userId, Response.Status.OK);
    }

    public static SimpleResponse getRequest(final RestEndpoint restEndpoint, final Map<String, String> queryParams, final String userId, final Response.Status expectedResponseCode) {
        final String url = createUrlWithParam(getBaseUri() + restEndpoint.getUri(), queryParams);
        final Response response = new RestClient().query(url, restEndpoint.getMediaType(), newHeadersMap(userId));
        final int responseStatus = response.getStatus();

        assertThat(responseStatus, equalTo(expectedResponseCode.getStatusCode()));

        return SimpleResponse.of(responseStatus, response.readEntity(String.class));
    }

    public static SimpleResponse getRequest(final String uri,
                                            final String mediaType,
                                            final UUID userId,
                                            final Response.Status expectedResponseCode) {

        final String url = getBaseUri() + uri;
        final Response response = new RestClient().query(url, mediaType, newHeadersMap(userId.toString()));
        final int responseStatus = response.getStatus();

        assertThat(responseStatus, equalTo(expectedResponseCode.getStatusCode()));

        return SimpleResponse.of(responseStatus, response.readEntity(String.class));
    }

    public static void postRequest(final String uri, final String mediaType, final String payload, final String userId) {
        final Response response = new RestClient().postCommand(getBaseUri() + uri, mediaType, payload, newHeadersMap(userId));
        assertThat(response.getStatus(), equalTo(Response.Status.ACCEPTED.getStatusCode()));
    }

    public static List<String> getFileContent(final String fileName) throws IOException {
        final String reportLocation = getBaseUri() + "/test/";
        final PollingRequestParams params = pollingRequestParams(reportLocation, "text/html")
                .withExpectedResponseStatus(Response.Status.OK)
                .withResponseBodyCondition(s -> s.contains(fileName))
                .withDelayInMillis(5000)
                .withRetryCount(5).build();
        new PollingRestClient().pollUntilExpectedResponse(params);

        final URL csvUrl = new URL(reportLocation + fileName);
        final BufferedReader reader = new BufferedReader(new InputStreamReader((csvUrl.openStream())));
        return reader.lines().collect(Collectors.toList());
    }

    private static String createUrlWithParam(final String url, final Map<String, String> queryParam) {
        if (queryParam.isEmpty()) {
            return url;
        }

        final StringBuilder sb = new StringBuilder();
        for (final Entry<String, String> e : queryParam.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            try {
                sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(e.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e1) {
                return null;
            }
        }
        return url.concat("?" + sb.toString());
    }

    private static MultivaluedMap<String, Object> newHeadersMap(final String userId) {
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);
        return map;
    }
}
