package uk.gov.moj.cpp.prosecution.documentqueue.it.test.command;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.DocumentQueueTableList.DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.waitUntilDataPersist;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentReviewRequired;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newReviewDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.FileServiceHelper.create;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.DELETE_DOCUMENTS_OF_CASES;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.UPDATE_DOCUMENT_STATUS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventListener;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;


public class DeleteDocumentsOfCasesIT extends BaseIT {

    private static final String DOCUMENT_STATUS_CHECK = "id = '%s' and status = '%s'";

    private UUID documentId;
    private static final UUID LEGAL_ADVISER = randomUUID();
    private static final UUID SYSTEM_USER = randomUUID();
    private UUID caseId = randomUUID();


    public static final String PUBLIC_DOCUMENT_STATUS_UPDATED = "public.documentqueue.document-status-updated";

    private static final String DOCUMENT_MARKED_DELETED = "documentqueue.event.document-marked-deleted";
    private static final String DOCUMENT_DELETED_FROM_FILE_STORE = "documentqueue.event.document-deleted-from-file-store";
    private static final String DELETE_DOCUMENTS_OF_CASES_REQUESTED = "documentqueue.event.delete-documents-of-cases-requested";
    private static final String DELETE_DOCUMENTS_OF_CASES_ACTIONED = "documentqueue.event.delete-documents-of-cases-actioned";
    private static final String CASE_PTI_URN = "ID01";

    @Inject
    private Logger logger;

    @BeforeClass
    public static void init() {
        setupAsAuthorisedUser(LEGAL_ADVISER, "Legal Advisers");
        setupAsAuthorisedUser(SYSTEM_USER, "System Users");
    }

    @Before
    public void setUp() throws Exception {
        stubIdMapperReturningExistingAssociation(UUID.randomUUID());
        final UUID fileStoreId = uploadFile("application/pdf");
        final Map<String, String> cpsDocumentValues = newReviewDocumentValues("CPS", "OTHER", fileStoreId);
        cpsDocumentValues.put("fileName", PUBLIC_DOCUMENT_REVIEW_REQUIRED);
        cpsDocumentValues.put("caseId", caseId.toString());
        newDocumentReviewRequired(cpsDocumentValues);
    }

    @Test
    public void shouldDeleteTheDocumentsOfCases() {
        // delete documents cases command
        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(3000)
                .subscribe(DOCUMENT_MARKED_DELETED)
                .subscribe(DELETE_DOCUMENTS_OF_CASES_REQUESTED)
                .subscribe(DELETE_DOCUMENTS_OF_CASES_ACTIONED)
                .subscribe(DOCUMENT_DELETED_FROM_FILE_STORE)
                .run(this::postDeleteDocumentsOfCases);

        final Optional<JsonEnvelope> documentMarkedDeletedEvent = eventListener.popEvent(DOCUMENT_MARKED_DELETED);
        final Optional<JsonEnvelope> deleteDocumentsOfCasesRequestedEvent = eventListener.popEvent(DELETE_DOCUMENTS_OF_CASES_REQUESTED);
        final Optional<JsonEnvelope> deleteDocumentsOfCasesActionedEvent = eventListener.popEvent(DELETE_DOCUMENTS_OF_CASES_ACTIONED);
        final Optional<JsonEnvelope> documentDeletedFromFileStore = eventListener.popEvent(DOCUMENT_DELETED_FROM_FILE_STORE);

        assertThat(documentMarkedDeletedEvent.isPresent(), is(true));
        assertThat(deleteDocumentsOfCasesRequestedEvent.isPresent(), is(true));
        assertThat(deleteDocumentsOfCasesActionedEvent.isPresent(), is(true));
        assertThat(documentDeletedFromFileStore.isPresent(), is(true));
    }

    private void updateDocumentStatus(final Status status) {
        MessageConsumer messageConsumerForDocumentStatusUpdated = postDocumentStatusUpdate(status, documentId, PUBLIC_DOCUMENT_STATUS_UPDATED);
        waitUntilDataPersist(DOCUMENT.toString(), format(DOCUMENT_STATUS_CHECK, documentId.toString(), status.toString()), 1);
        verifyDocumentStatusUpdatedPublicEventReceived(documentId, status, messageConsumerForDocumentStatusUpdated);
    }

    private static MessageConsumer postDocumentStatusUpdate(final Status status,
                                                            final UUID documentId,
                                                            final String eventSelector) {
        final MessageConsumer messageConsumerForDocumentStatusUpdated = publicEvents.createConsumer(eventSelector);
        postRequest(format(UPDATE_DOCUMENT_STATUS.getUri(), documentId.toString()),
                UPDATE_DOCUMENT_STATUS.getMediaType(),
                createObjectBuilder()
                        .add("status",
                                status.toString())
                        .build()
                        .toString(),
                LEGAL_ADVISER.toString());
        return messageConsumerForDocumentStatusUpdated;
    }

    private void postDeleteDocumentsOfCases() {
        postRequest(DELETE_DOCUMENTS_OF_CASES.getUri(),
                DELETE_DOCUMENTS_OF_CASES.getMediaType(),
                createObjectBuilder().add("casePTIUrns", createArrayBuilder()
                        .add("ID01")
                        .build())
                        .build().toString(),
                SYSTEM_USER.toString());
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

    private void verifyDocumentDeletedFromFileStorePrivateEventReceived(final UUID documentId,
                                                                final MessageConsumer messageConsumerForDocumentStatusUpdated) {
        final JsonPath topicMessage = privateEvents.retrieveMessage(messageConsumerForDocumentStatusUpdated);
        final String payloadDocumentId = topicMessage.getJsonObject("documentId");
        final String fileServiceId = topicMessage.getJsonObject("fileServiceId");

        assertThat(payloadDocumentId, is(documentId.toString()));
        assertThat(fileServiceId, is(notNullValue()));
        try {
            messageConsumerForDocumentStatusUpdated.close();
        } catch (JMSException e) {
            logger.error("Error while closing Message consumer: {}", e);
        }
    }

    public static UUID uploadFile(final String mimeType) throws Exception {
        return create("XVBN22.pdf", mimeType, getSystemResourceAsStream("materials/XVBN22.pdf"));
    }

}
