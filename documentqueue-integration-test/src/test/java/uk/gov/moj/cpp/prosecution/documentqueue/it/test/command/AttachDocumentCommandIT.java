package uk.gov.moj.cpp.prosecution.documentqueue.it.test.command;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentReviewRequired;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newReviewDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.FileServiceHelper.create;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.SimpleFileClient.getFile;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.ATTACH_DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames.CONTEXT_NAME;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.*;

public class AttachDocumentCommandIT extends BaseIT {
    private final static String PDF_MIME_TYPE = "application/pdf";
    private static final UUID userId = randomUUID();


    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(userId, "Legal Advisers");
        stubForIdMapperSuccess(Response.Status.ACCEPTED);
        stubForCourtDocumentSuccess(Response.Status.ACCEPTED);
        stubForMaterialSuccess(Response.Status.ACCEPTED);
    }

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void shouldSuccessfulyAttachDocument() throws Exception {
        final JmsMessageConsumerClient documentAlreadyAttachedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.DOCUMENT_ALREADY_ATTACHED).getMessageConsumerClient();
        final JmsMessageConsumerClient documentAttachedPublicConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(EventNames.PUBLIC_DOCUMENT_ATTACHED).getMessageConsumerClient();
        final JmsMessageConsumerClient attachDocumentRequestedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames( EventNames.ATTACH_DOCUMENT_REQUESTED).getMessageConsumerClient();

        // Create CPS document
        final UUID documentId = uploadFile(PDF_MIME_TYPE);
        final Map<String, String> cpsDocumentValues = newReviewDocumentValues("CPS", "OTHER", documentId);
        cpsDocumentValues.put("fileName", PUBLIC_DOCUMENT_REVIEW_REQUIRED);
        newDocumentReviewRequired(cpsDocumentValues);

        postAttachDocument(documentId);

        verifyDocumentAttached(documentId, documentAttachedPublicConsumer);
        verifyAttachDocumentRequested(documentId, attachDocumentRequestedConsumer);
        verifyDocumentAlreadyAttachedNotRaised(documentAlreadyAttachedConsumer); //TODO this not a nice way to test as in happy path scenario test will wait for 20 secs timeout to retrieve message

        // Now send another attach document event
        postAttachDocument(documentId);

        verifyDocumentAlreadyAttached(documentId, documentAlreadyAttachedConsumer);
    }

    private static UUID uploadFile(final String mimeType) throws Exception {
        return create("XVBN22.pdf", mimeType, getSystemResourceAsStream("materials/XVBN22.pdf"));
    }

    private static void postAttachDocument(UUID documentId) {
        final Map<String, String> values = new HashMap<>();
        values.put("courtDocumentId", documentId.toString());
        postRequest(format(ATTACH_DOCUMENT.getUri(), documentId.toString()),
                ATTACH_DOCUMENT.getMediaType(),
                getFile("json/" + EventPayloadUtil.ATTACH_DOCUMENT, values).asString(),
                userId.toString());
    }

    private static void verifyDocumentAttached(UUID documentId, JmsMessageConsumerClient mc2) throws Exception {
        final JsonPath topicMessage = mc2.retrieveMessageAsJsonPath().get();
        final String payloadDocumentId = topicMessage.getString("documentId");
        assertThat(payloadDocumentId, is(documentId.toString()));
    }

    private static void verifyAttachDocumentRequested(UUID documentId, JmsMessageConsumerClient mc) throws Exception {
        final JsonPath topicMessage = mc.retrieveMessageAsJsonPath().get();
        assertThat(topicMessage, notNullValue());

        final String payloadDocumentId = topicMessage.getString("documentId");
        final HashMap<String, Object> payloadCourtDocument = topicMessage.getJsonObject("courtDocument");
        assertThat(payloadDocumentId, is(documentId.toString()));
        assertThat(payloadCourtDocument.get("courtDocumentId"), is(documentId.toString()));
    }

    private static void verifyDocumentAlreadyAttachedNotRaised(JmsMessageConsumerClient mc) throws Exception {
        assertTrue(mc.retrieveMessageAsJsonPath(1_000).isEmpty());
    }

    private static void verifyDocumentAlreadyAttached(UUID documentId, JmsMessageConsumerClient mc) throws Exception {
        final JsonPath topicMessage = mc.retrieveMessageAsJsonPath().get();
        assertThat(topicMessage, notNullValue());

        final String payloadDocumentId = topicMessage.getString("documentId");
        assertThat(payloadDocumentId, is(documentId.toString()));
    }
}
