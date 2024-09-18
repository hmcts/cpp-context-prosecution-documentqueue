package uk.gov.moj.cpp.prosecution.documentqueue.it.test.command;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentReviewRequired;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newReviewDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.FileServiceHelper.create;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.DELETE_DOCUMENTS_OF_CASES;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames.CONTEXT_NAME;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;


public class DeleteDocumentsOfCasesIT extends BaseIT {

    private UUID documentId;
    private static final UUID LEGAL_ADVISER = randomUUID();
    private static final UUID SYSTEM_USER = randomUUID();
    private UUID caseId = randomUUID();

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
    }

    @Test
    public void shouldDeleteTheDocumentsOfCases() throws Exception {
        final JmsMessageConsumerClient docMarkedDeletedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_MARKED_DELETED).getMessageConsumerClient();
        final JmsMessageConsumerClient deleteDocsCasRequestedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DELETE_DOCUMENTS_OF_CASES_REQUESTED).getMessageConsumerClient();
        final JmsMessageConsumerClient delDocsCaseActionedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DELETE_DOCUMENTS_OF_CASES_ACTIONED).getMessageConsumerClient();
        final JmsMessageConsumerClient docDeletedFromFSConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_DELETED_FROM_FILE_STORE).getMessageConsumerClient();

        postDeleteDocumentsOfCases();

        final Optional<JsonEnvelope> documentMarkedDeletedEvent = docMarkedDeletedConsumer.retrieveMessageAsJsonEnvelope();
        final Optional<JsonEnvelope> deleteDocumentsOfCasesRequestedEvent = deleteDocsCasRequestedConsumer.retrieveMessageAsJsonEnvelope();
        final Optional<JsonEnvelope> deleteDocumentsOfCasesActionedEvent = delDocsCaseActionedConsumer.retrieveMessageAsJsonEnvelope();
        final Optional<JsonEnvelope> documentDeletedFromFileStore = docDeletedFromFSConsumer.retrieveMessageAsJsonEnvelope();

        assertThat(documentMarkedDeletedEvent.isPresent(), is(true));
        assertThat(deleteDocumentsOfCasesRequestedEvent.isPresent(), is(true));
        assertThat(deleteDocumentsOfCasesActionedEvent.isPresent(), is(true));
        assertThat(documentDeletedFromFileStore.isPresent(), is(true));
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

    public static UUID uploadFile(final String mimeType) throws Exception {
        return create("XVBN22.pdf", mimeType, getSystemResourceAsStream("materials/XVBN22.pdf"));
    }

}
