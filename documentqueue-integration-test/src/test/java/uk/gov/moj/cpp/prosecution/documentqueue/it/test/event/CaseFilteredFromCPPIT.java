package uk.gov.moj.cpp.prosecution.documentqueue.it.test.event;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.JmsMessageSender;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentReviewRequired;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newReviewDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames.CONTEXT_NAME;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.test.command.DeleteDocumentsOfCasesIT.uploadFile;


public class CaseFilteredFromCPPIT extends BaseIT {

    private final static String PDF_MIME_TYPE = "application/pdf";

    private UUID documentId;
    private UUID caseId = randomUUID();
    private final JmsMessageSender jmsMessageSender = new JmsMessageSender();
    private static final UUID userId = randomUUID();


    @Inject
    private Logger logger;

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(userId, "Legal Advisers");
    }

    @BeforeEach
    public void setUp() {
        stubIdMapperReturningExistingAssociation(caseId);
    }

    @Test
    public void testDeleteDocumentWhenInProgress() throws Exception {
        final JmsMessageConsumerClient docLinkedToCaseConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_DOCUMENT_LINKED_TO_CASE).getMessageConsumerClient();
        final JmsMessageConsumerClient docOutStandingDocReceivedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_OUT_STANDING_DOCUMENT_RECEIVED).getMessageConsumerClient();
        final JmsMessageConsumerClient caseMarkedFilteredConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.CASE_MARKED_FILTERED).getMessageConsumerClient();
        final JmsMessageConsumerClient docMarkedDeletedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_MARKED_DELETED).getMessageConsumerClient();
        final JmsMessageConsumerClient docDeleteFromFSRequestedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_DELETE_FROM_FILE_STORE_REQUESTED).getMessageConsumerClient();
        final JmsMessageConsumerClient docDeletedFromFSConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_DELETED_FROM_FILE_STORE).getMessageConsumerClient();

        createCPSDocument();

        final Optional<JsonEnvelope> documentLinkedToCase = docLinkedToCaseConsumer.retrieveMessageAsJsonEnvelope();
        final Optional<JsonEnvelope> outStandingDocumentReceived = docOutStandingDocReceivedConsumer.retrieveMessageAsJsonEnvelope();
        assertThat(documentLinkedToCase.isPresent(), is(true));
        assertThat(outStandingDocumentReceived.isPresent(), is(true));

        raiseCaseFilteredEvent();


        final Optional<JsonEnvelope> caseMarkedFiltered = caseMarkedFilteredConsumer.retrieveMessageAsJsonEnvelope();
        final Optional<JsonEnvelope> documentMarkedDeleted = docMarkedDeletedConsumer.retrieveMessageAsJsonEnvelope();
        final Optional<JsonEnvelope> documentDeleteFromFileStoreRequested = docDeleteFromFSRequestedConsumer.retrieveMessageAsJsonEnvelope();
        final Optional<JsonEnvelope> documentDeletedFromFileStore = docDeletedFromFSConsumer.retrieveMessageAsJsonEnvelope();


        assertThat(caseMarkedFiltered.isPresent(), is(true));
        assertThat(documentMarkedDeleted.isPresent(), is(true));
        assertThat(documentDeleteFromFileStoreRequested.isPresent(), is(true));
        assertThat(documentDeletedFromFileStore.isPresent(), is(true));
    }

    private void createCPSDocument() {
        // Create CPS document
        final UUID documentId;
        try {
            documentId = uploadFile(PDF_MIME_TYPE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final Map<String, String> cpsDocumentValues = newReviewDocumentValues("CPS", "OTHER", documentId);
        cpsDocumentValues.put("fileName", PUBLIC_DOCUMENT_REVIEW_REQUIRED);
        cpsDocumentValues.put("caseId", caseId.toString());
        newDocumentReviewRequired(cpsDocumentValues);
    }


    private void raiseCaseFilteredEvent() {
        final Map<String, String> valueMap = new HashMap<>();
        valueMap.put("caseId", caseId.toString());
        jmsMessageSender.sendPublicEvent(
                EventNames.PUBLIC_CASE_OR_APPLICATION_FILTERED,
                "documentqueue/public.stagingprosecutorsspi.event.prosecution-case-filtered.json",
                valueMap,
                ZonedDateTime.now());
    }
}
