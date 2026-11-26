package uk.gov.moj.cpp.prosecution.documentqueue.it.test.event;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.JmsMessageSender;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.DocumentQueueTableList.DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.waitUntilDataPersist;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newScanEnvelopeRegistered;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.UPDATE_DOCUMENT_STATUS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames.CONTEXT_NAME;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_SCAN_ENVELOPE_REGISTERED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.*;

public class CaseEjectedFromCPPIT extends BaseIT {

    private static final String DOCUMENT_STATUS_CHECK = "id = '%s' and status = '%s'";

    private UUID documentId;
    private UUID caseId;
    private static final UUID userId = randomUUID();

    private final JmsMessageSender jmsMessageSender = new JmsMessageSender();

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
        caseId = randomUUID();
        stubIdMapperReturningExistingAssociation(caseId);
        stubForIdMapperSuccess(Response.Status.OK);
        newScanEnvelopeRegistered(values);
        documentId = fromString(values.get("documentId"));
    }


    @Test
    public void testDeleteDocumentWhenInProgress()  throws Exception {
        final JmsMessageConsumerClient docMarkedCompletedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_MARKED_COMPLETED).getMessageConsumerClient();
        final JmsMessageConsumerClient docStatusUpdatedPublicConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(EventNames.PUBLIC_DOCUMENT_STATUS_UPDATED).getMessageConsumerClient();
        updateDocumentStatus(IN_PROGRESS, docStatusUpdatedPublicConsumer);
        final Map<String, String> valueMap = new HashMap<>();
        valueMap.put("caseId", caseId.toString());

        jmsMessageSender.sendPublicEvent(
                EventNames.PUBLIC_CASE_OR_APPLICATION_EJECTED,
                "documentqueue/public.progression.events.case-or-application-ejected.json",
                valueMap,
                ZonedDateTime.now());

        var eventPayload = docMarkedCompletedConsumer.retrieveMessageAsJsonPath().get();
        assertThat("Document Id does not match",
                eventPayload
                        .getJsonObject("documentId")
                        .equals(documentId.toString()));
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

    private void verifyDocumentStatusUpdatedPublicEventReceived(final UUID documentId,
                                                                final Status status,
                                                                final JmsMessageConsumerClient mc)  throws Exception {
        final JsonPath topicMessage = mc.retrieveMessageAsJsonPath().get();
        final String payloadDocumentId = topicMessage.getJsonObject("documentId");
        final String payloadStatus =  topicMessage.getJsonObject( "status");
        assertThat(payloadDocumentId, is(documentId.toString()));
        assertThat(payloadStatus, is(status.toString()));
    }
}
