package uk.gov.moj.cpp.prosecution.documentqueue.it.test.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentReviewRequired;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newReviewDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventType.PUBLIC_EVENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.SimpleMessageQueueClient.publicEventToTopic;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubGetReferenceDataBySectionCode;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.test.command.DeleteDocumentsOfCasesIT.uploadFile;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventListener;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;


public class DeleteDocumentsQueueIT extends BaseIT {

    private final static String PDF_MIME_TYPE = "application/pdf";

    private UUID documentId;
    private UUID caseId = randomUUID();
    private static final UUID userId = randomUUID();

    // document
    private static final String DOCUMENT_STATUS_UPDATED = "documentqueue.event.document-status-updated";

    private static final String DOCUMENT_DOCUMENT_LINKED_TO_CASE = "documentqueue.event.document-linked-to-case";
    private static final String DOCUMENT_OUT_STANDING_DOCUMENT_RECEIVED = "documentqueue.event.outstanding-document-received";

    private static final String PUBLIC_PROSECUTION_CASE_FILE_MATERIAL_ADDED = "public.prosecutioncasefile.material-added";

    @Inject
    private Logger logger;

    @BeforeClass
    public static void init() {
        setupAsAuthorisedUser(userId, "Legal Advisers");
    }

    @Before
    public void setUp() {
        stubIdMapperReturningExistingAssociation(caseId);
        stubGetReferenceDataBySectionCode();
    }

    @Test
    public void testDeleteDocumentWhenInProgress() {

        final EventListener eventListener1 = new EventListener()
                .withMaxWaitTime(3000)
                .subscribe(DOCUMENT_DOCUMENT_LINKED_TO_CASE)
                .subscribe(DOCUMENT_OUT_STANDING_DOCUMENT_RECEIVED)
                .run(this::createCPSDocument);

        final Optional<JsonEnvelope> documentLinkedToCase = eventListener1.popEvent(DOCUMENT_DOCUMENT_LINKED_TO_CASE);
        final Optional<JsonEnvelope> outStandingDocumentReceived = eventListener1.popEvent(DOCUMENT_OUT_STANDING_DOCUMENT_RECEIVED);
        assertThat(documentLinkedToCase.isPresent(), is(true));
        assertThat(outStandingDocumentReceived.isPresent(), is(true));

        // raise material added event
        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(3000)
                .subscribe(DOCUMENT_STATUS_UPDATED)
                .run(this::raiseMaterialAddedEvent);

        final Optional<JsonEnvelope> DOCUMENT_STATUS_UPDATED = eventListener.popEvent(DeleteDocumentsQueueIT.DOCUMENT_STATUS_UPDATED);

        assertThat(DOCUMENT_STATUS_UPDATED.isPresent(), is(true));
    }

    private void createCPSDocument() {
        // Create CPS document
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


    private void raiseMaterialAddedEvent() {

        final Map<String, String> valueMap = new HashMap<>();
        valueMap.put("caseId", caseId.toString());
        valueMap.put("fileStoreId", documentId.toString());

        publicEventToTopic(
                PUBLIC_PROSECUTION_CASE_FILE_MATERIAL_ADDED,
                PUBLIC_EVENT.getStringValue(),
                "documentqueue/public.prosecutioncasefile.material-added.json",
                valueMap,
                ZonedDateTime.now());
    }

}
