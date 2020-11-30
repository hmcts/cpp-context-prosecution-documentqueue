package uk.gov.moj.cpp.prosecution.documentqueue.it.test.command;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.DocumentQueueTableList.DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.waitUntilDataPersist;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentReviewRequired;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newReviewDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.waitUntilFileDeletedStatusIsUpdated;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.FileServiceHelper.create;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.DELETE_EXPIRED_DOCUMENTS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.UPDATE_DOCUMENT_STATUS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.test.command.DeleteDocumentsOfCasesIT.PUBLIC_DOCUMENT_STATUS_UPDATED;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventListener;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonString;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;


public class DeleteExpiredDocumentsIT {

    private String documentId;
    private static final UUID LEGAL_ADVISER = randomUUID();
    private static final UUID SYSTEM_USER = randomUUID();
    private UUID caseId = randomUUID();

    private static final String DELETE_EXPIRED_DOCUMENTS_RECEIVED = "documentqueue.event.delete-expired-documents-request-received";
    private static final String DELETE_EXPIRED_DOCUMENTS_REQUESTED = "documentqueue.event.delete-expired-documents-requested";
    private static final String DOCUMENT_MARKED_COMPLETED = "documentqueue.event.document-marked-completed";
    private static final String DOCUMENT_MARKED_FILE_DELETED = "documentqueue.event.document-marked-file-deleted";
    private static final String CASE_PTI_URN = "ID01";
    private static final String DOCUMENT_STATUS_CHECK = "id = '%s' and status = '%s'";

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
        documentId = cpsDocumentValues.get("fileStoreId");
    }

    @Test
    public void shouldDeleteExpiredDocuments() {
        // delete documents cases command
        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(3000)
                .subscribe(DELETE_EXPIRED_DOCUMENTS_RECEIVED)
                .subscribe(DELETE_EXPIRED_DOCUMENTS_REQUESTED)
                .subscribe(DOCUMENT_MARKED_COMPLETED)
                .run(this::postDeleteExpiredDocumentsOfCases);

        Optional<JsonEnvelope> deleteExpiredDocumentsReceived = eventListener.popEvent(DELETE_EXPIRED_DOCUMENTS_RECEIVED);
        Optional<JsonEnvelope> deleteExpiredDocumentsRequired = eventListener.popEvent(DELETE_EXPIRED_DOCUMENTS_REQUESTED);

        assertThat(deleteExpiredDocumentsReceived.isPresent(), is(true));
        assertThat(deleteExpiredDocumentsRequired.isPresent(), is(true));
        assertThat(deleteExpiredDocumentsRequired.get().payloadAsJsonObject().getJsonArray("documentIds").size(), greaterThan(0));

        final long matchedCount = deleteExpiredDocumentsRequired
                .get()
                .payloadAsJsonObject()
                .getJsonArray("documentIds")
                .stream()
                .map(e -> ((JsonString) e).getString())
                .filter(documentId::equals)
                .count();
        assertThat(matchedCount, is(1l));

        final Optional<JsonEnvelope> documentMarkedCompleted = eventListener.popEvent(DOCUMENT_MARKED_COMPLETED);
        assertThat(deleteExpiredDocumentsReceived.isPresent(), is(true));

        final String docId = documentMarkedCompleted.get().payloadAsJsonObject().getString("documentId");
        assertThat(docId, equalTo(documentId));
    }

    @Test
    public void shouldDeleteDocumentsFromFileStore() {

        updateDocumentStatus(Status.IN_PROGRESS);

        updateDocumentStatus(Status.COMPLETED);

        // delete documents cases command
        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(10000)
                .subscribe(DOCUMENT_MARKED_FILE_DELETED)
                .run(this::postDeleteExpiredDocumentsOfCases);

        Optional<JsonEnvelope> documentMarkedFileDeletedEnvelope = eventListener.popEvent(DOCUMENT_MARKED_FILE_DELETED);

        assertThat(documentMarkedFileDeletedEnvelope.isPresent(), is(true));

        waitUntilFileDeletedStatusIsUpdated(documentId);
    }

    private void postDeleteExpiredDocumentsOfCases() {
        postRequest(DELETE_EXPIRED_DOCUMENTS.getUri(),
                DELETE_EXPIRED_DOCUMENTS.getMediaType(),
                null,
                SYSTEM_USER.toString());
    }

    private void updateDocumentStatus(final Status status) {
        MessageConsumer messageConsumerForDocumentStatusUpdated = postDocumentStatusUpdate(status, UUID.fromString(documentId), PUBLIC_DOCUMENT_STATUS_UPDATED);
        waitUntilDataPersist(DOCUMENT.toString(), format(DOCUMENT_STATUS_CHECK, documentId.toString(), status.toString()), 1);
        verifyDocumentStatusUpdatedPublicEventReceived(UUID.fromString(documentId), status, messageConsumerForDocumentStatusUpdated);
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

    private static UUID uploadFile(final String mimeType) throws Exception {
        return create("XVBN22.pdf", mimeType, getSystemResourceAsStream("materials/XVBN22.pdf"));
    }
}
