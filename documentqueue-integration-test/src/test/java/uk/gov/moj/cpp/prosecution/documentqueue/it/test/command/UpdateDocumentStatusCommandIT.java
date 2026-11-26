package uk.gov.moj.cpp.prosecution.documentqueue.it.test.command;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.*;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.DocumentQueueTableList.DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.waitUntilDataPersist;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newScanEnvelopeRegistered;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.UPDATE_DOCUMENT_STATUS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_SCAN_ENVELOPE_REGISTERED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;

public class UpdateDocumentStatusCommandIT extends BaseIT {

    private static final String DOCUMENT_STATUS_CHECK = "id = '%s' and status = '%s'";

    private UUID documentId;
    private static final UUID userId = randomUUID();

    @Inject
    private Logger logger;

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(userId, "Legal Advisers");
    }

    @BeforeEach
    public void setUp() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "APPLICATION", "FOLLOW_UP");
        values.put("fileName", PUBLIC_SCAN_ENVELOPE_REGISTERED);
        stubIdMapperReturningExistingAssociation(UUID.randomUUID());
        newScanEnvelopeRegistered(values);
        documentId = fromString(values.get("documentId"));
    }

    @Test
    public void testUpdateDocumentStatusToInProgressCommand() throws Exception {
        final JmsMessageConsumerClient consumer = newPublicJmsMessageConsumerClientProvider().withEventNames(EventNames.PUBLIC_DOCUMENT_STATUS_UPDATED).getMessageConsumerClient();
        updateDocumentStatus(IN_PROGRESS, consumer);
    }

    @Test
    public void testUpdateDocumentStatusToCompletedCommand() throws Exception {
        final JmsMessageConsumerClient consumer = newPublicJmsMessageConsumerClientProvider().withEventNames(EventNames.PUBLIC_DOCUMENT_STATUS_UPDATED).getMessageConsumerClient();
        updateDocumentStatus(IN_PROGRESS, consumer);
        updateDocumentStatus(COMPLETED, consumer);
    }

    @Test
    public void testUpdateDocumentStatusToOutstandingCommand() throws Exception {
        final JmsMessageConsumerClient consumer = newPublicJmsMessageConsumerClientProvider().withEventNames(EventNames.PUBLIC_DOCUMENT_STATUS_UPDATED).getMessageConsumerClient();
        updateDocumentStatus(IN_PROGRESS, consumer);
        updateDocumentStatus(OUTSTANDING, consumer);
    }

    @Test
    public void shouldFailOnConsecutiveInProgressUpdates() throws Exception {
        final JmsMessageConsumerClient docStatusUpdatedPublicConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(EventNames.PUBLIC_DOCUMENT_STATUS_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient docStatusUpdateFailedPublicConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(EventNames.PUBLIC_DOCUMENT_STATUS_UPDATE_FAILED).getMessageConsumerClient();
        failUpdateDocumentStatus(IN_PROGRESS,IN_PROGRESS, docStatusUpdatedPublicConsumer, docStatusUpdateFailedPublicConsumer);
    }

    private void failUpdateDocumentStatus(Status initialStatus,Status nextStatus, JmsMessageConsumerClient mc1, JmsMessageConsumerClient mc2) throws Exception {
        postDocumentStatusUpdate(initialStatus,documentId);
        verifyDocumentStatusUpdatedPublicEventReceived(documentId,initialStatus, mc1);
        postDocumentStatusUpdate(nextStatus,documentId);
        verifyDocumentStatusUpdatedPublicEventReceived(documentId,nextStatus, mc2);
    }

    private void updateDocumentStatus(final Status status, final JmsMessageConsumerClient mc) throws Exception {
        postDocumentStatusUpdate(status, documentId);
        waitUntilDataPersist(DOCUMENT.toString(), format(DOCUMENT_STATUS_CHECK, documentId.toString(), status.toString()), 1);
        verifyDocumentStatusUpdatedPublicEventReceived(documentId, status, mc);
    }

    private static void postDocumentStatusUpdate(final Status status, final UUID documentId) {
        postRequest(format(UPDATE_DOCUMENT_STATUS.getUri(), documentId.toString()),
                UPDATE_DOCUMENT_STATUS.getMediaType(),
                createObjectBuilder().add("status", status.toString()).build().toString(),
                userId.toString());
    }

    private void verifyDocumentStatusUpdatedPublicEventReceived(final UUID documentId, final Status status, final JmsMessageConsumerClient mc) throws Exception {
        final JsonPath topicMessage = mc.retrieveMessageAsJsonPath().get();
        final String payloadDocumentId = topicMessage.getJsonObject("documentId");
        final String payloadStatus =  topicMessage.getJsonObject( "status");
        assertThat(payloadDocumentId, is(documentId.toString()));
        assertThat(payloadStatus, is(status.toString()));
    }

}
