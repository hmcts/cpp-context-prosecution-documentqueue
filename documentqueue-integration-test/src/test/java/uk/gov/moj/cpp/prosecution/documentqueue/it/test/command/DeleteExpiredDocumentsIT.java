package uk.gov.moj.cpp.prosecution.documentqueue.it.test.command;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import javax.inject.Inject;
import javax.json.JsonString;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.DocumentQueueTableList.DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.waitUntilDataPersist;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.*;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.FileServiceHelper.create;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.DELETE_EXPIRED_DOCUMENTS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.UPDATE_DOCUMENT_STATUS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames.CONTEXT_NAME;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;


public class DeleteExpiredDocumentsIT extends BaseIT {

    private String documentId;
    private static final UUID LEGAL_ADVISER = randomUUID();
    private static final UUID SYSTEM_USER = randomUUID();
    private UUID caseId = randomUUID();

    private static final String DOCUMENT_STATUS_CHECK = "id = '%s' and status = '%s'";

    @Inject
    private Logger logger;

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(LEGAL_ADVISER, "Legal Advisers");
        setupAsAuthorisedUser(SYSTEM_USER, "System Users");
    }

    @BeforeEach
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
    public void shouldDeleteExpiredDocuments() throws Exception {
        final JmsMessageConsumerClient delExpDocsReceivedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DELETE_EXPIRED_DOCUMENTS_RECEIVED).getMessageConsumerClient();
        final JmsMessageConsumerClient delExpDocsRequestedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DELETE_EXPIRED_DOCUMENTS_REQUESTED).getMessageConsumerClient();
        final JmsMessageConsumerClient docMarkedCompletedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_MARKED_COMPLETED).getMessageConsumerClient();

        // delete documents cases command
        postDeleteExpiredDocumentsOfCases();

        Optional<JsonEnvelope> deleteExpiredDocumentsReceived = delExpDocsReceivedConsumer.retrieveMessageAsJsonEnvelope();
        Optional<JsonEnvelope> deleteExpiredDocumentsRequired = delExpDocsRequestedConsumer.retrieveMessageAsJsonEnvelope();

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

        final Optional<JsonEnvelope> documentMarkedCompleted = docMarkedCompletedConsumer.retrieveMessageAsJsonEnvelope();
        assertThat(deleteExpiredDocumentsReceived.isPresent(), is(true));

        final String docId = documentMarkedCompleted.get().payloadAsJsonObject().getString("documentId");
        assertThat(docId, equalTo(documentId));
    }


    @Test
    public void shouldDeleteDocumentsFromFileStore() throws Exception {
        final JmsMessageConsumerClient docMarkedFileDeletedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_MARKED_FILE_DELETED).getMessageConsumerClient();
        final JmsMessageConsumerClient docStatusUpdatedPublicConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(EventNames.PUBLIC_DOCUMENT_STATUS_UPDATED).getMessageConsumerClient();

        updateDocumentStatus(Status.IN_PROGRESS, docStatusUpdatedPublicConsumer);

        updateDocumentStatus(Status.COMPLETED, docStatusUpdatedPublicConsumer);

        // delete documents cases command
        postDeleteExpiredDocumentsOfCases();

        Optional<JsonEnvelope> documentMarkedFileDeletedEnvelope = docMarkedFileDeletedConsumer.retrieveMessageAsJsonEnvelope();

        assertThat(documentMarkedFileDeletedEnvelope.isPresent(), is(true));

        waitUntilFileDeletedStatusIsUpdated(documentId);
    }

    private void postDeleteExpiredDocumentsOfCases() {
        postRequest(DELETE_EXPIRED_DOCUMENTS.getUri(),
                DELETE_EXPIRED_DOCUMENTS.getMediaType(),
                null,
                SYSTEM_USER.toString());
    }

    private void updateDocumentStatus(final Status status, final JmsMessageConsumerClient mc) throws Exception {
        postDocumentStatusUpdate(status, UUID.fromString(documentId));
        waitUntilDataPersist(DOCUMENT.toString(), format(DOCUMENT_STATUS_CHECK, documentId, status), 1);
        verifyDocumentStatusUpdatedPublicEventReceived(UUID.fromString(documentId), status, mc);
    }

    private static void postDocumentStatusUpdate(final Status status,
                                                            final UUID documentId) {
        postRequest(format(UPDATE_DOCUMENT_STATUS.getUri(), documentId.toString()),
                UPDATE_DOCUMENT_STATUS.getMediaType(),
                createObjectBuilder()
                        .add("status",
                                status.toString())
                        .build()
                        .toString(),
                LEGAL_ADVISER.toString());
    }

    private void verifyDocumentStatusUpdatedPublicEventReceived(final UUID documentId,
                                                                final Status status,
                                                                final JmsMessageConsumerClient mc) throws Exception {
        final JsonPath topicMessage = mc.retrieveMessageAsJsonPath().get();
        final String payloadDocumentId = topicMessage.getJsonObject("documentId");
        final String payloadStatus =  topicMessage.getJsonObject( "status");
        assertThat(payloadDocumentId, is(documentId.toString()));
        assertThat(payloadStatus, is(status.toString()));
    }

    private static UUID uploadFile(final String mimeType) throws Exception {
        return create("XVBN22.pdf", mimeType, getSystemResourceAsStream("materials/XVBN22.pdf"));
    }
}
