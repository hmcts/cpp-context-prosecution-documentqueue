package uk.gov.moj.cpp.prosecution.documentqueue.it.test.event;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.DocumentQueueTableList.DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.waitUntilDataPersist;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newScanEnvelopeRegistered;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventType.PUBLIC_EVENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.SimpleMessageQueueClient.publicEventToTopic;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.UPDATE_DOCUMENT_STATUS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_SCAN_ENVELOPE_REGISTERED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

public class CaseEjectedFromCPPIT extends BaseIT {

    private static final String DOCUMENT_STATUS_CHECK = "id = '%s' and status = '%s'";

    private UUID documentId;
    private UUID caseId;
    private static final UUID userId = randomUUID();
    private static final String PUBLIC_DOCUMENT_STATUS_UPDATED = "public.documentqueue.document-status-updated";
    private static final String PUBLIC_CASE_OR_APPLICATION_EJECTED ="public.progression.events.case-or-application-ejected";

    @Inject
    private Logger logger;

    @BeforeClass
    public static void init() {
        setupAsAuthorisedUser(userId, "Legal Advisers");
    }

    @Before
    public void setUp() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "APPLICATION", "FOLLOW_UP");
        values.put("fileName", PUBLIC_SCAN_ENVELOPE_REGISTERED);
        caseId = randomUUID();
        stubIdMapperReturningExistingAssociation(caseId);
        newScanEnvelopeRegistered(values);
        documentId = fromString(values.get("documentId"));
    }


    @Test
    public void testDeleteDocumentWhenInProgress() {
        updateDocumentStatus(IN_PROGRESS);
        final Map<String, String> valueMap = new HashMap<>();
        valueMap.put("caseId", caseId.toString());
        final MessageConsumer messageConsumer = privateEvents.createConsumer("documentqueue.event.document-marked-completed");

        publicEventToTopic(
                PUBLIC_CASE_OR_APPLICATION_EJECTED,
                PUBLIC_EVENT.getStringValue(),
                "documentqueue/public.progression.events.case-or-application-ejected.json",
                valueMap,
                ZonedDateTime.now());

        assertThat("Document Id does not match",
                privateEvents.retrieveMessage(messageConsumer)
                        .getJsonObject("documentId")
                        .equals(documentId.toString()));
     }

    private void updateDocumentStatus(final Status status) {
        MessageConsumer messageConsumerForDocumentStatusUpdated = postDocumentStatusUpdate(status, documentId,PUBLIC_DOCUMENT_STATUS_UPDATED);
        waitUntilDataPersist(DOCUMENT.toString(), format(DOCUMENT_STATUS_CHECK, documentId.toString(), status.toString()), 1);
        verifyDocumentStatusUpdatedPublicEventReceived(documentId, status, messageConsumerForDocumentStatusUpdated);
    }
    private static MessageConsumer postDocumentStatusUpdate(final Status status, final UUID documentId,final String eventSelector) {
        MessageConsumer messageConsumerForDocumentStatusUpdated = publicEvents.createConsumer(eventSelector);
        postRequest(format(UPDATE_DOCUMENT_STATUS.getUri(), documentId.toString()),
                UPDATE_DOCUMENT_STATUS.getMediaType(),
                createObjectBuilder().add("status", status.toString()).build().toString(),
                userId.toString());
        return messageConsumerForDocumentStatusUpdated;
    }

    private void verifyDocumentStatusUpdatedPublicEventReceived(final UUID documentId,
                                                                final Status status,
                                                                final MessageConsumer messageConsumerForDocumentStatusUpdated) {
        final JsonPath topicMessage = publicEvents.retrieveMessage(messageConsumerForDocumentStatusUpdated);
        final String payloadDocumentId = topicMessage.getJsonObject("documentId");
        final String payloadStatus =  topicMessage.getJsonObject( "status");
        assertThat(payloadDocumentId, is(documentId.toString()));
        assertThat(payloadStatus, is(status.toString()));
        try {
            messageConsumerForDocumentStatusUpdated.close();
        } catch (JMSException e) {
            logger.error("Error while closing Message consumer: {}", e);
        }
    }
}
