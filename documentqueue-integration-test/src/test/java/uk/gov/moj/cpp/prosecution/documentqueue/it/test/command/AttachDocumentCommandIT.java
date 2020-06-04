package uk.gov.moj.cpp.prosecution.documentqueue.it.test.command;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentReviewRequired;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newReviewDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.FileServiceHelper.create;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.SimpleFileClient.getFile;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.RestEndpoint.ATTACH_DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.postRequest;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.QueueUtil;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("23-05-2020 Ignored while Jenkins build pipeline not setup with fileservice database")
public class AttachDocumentCommandIT extends BaseIT {
    private final static String PDF_MIME_TYPE = "application/pdf";
    private static final String PUBLIC_DOCUMENT_QUEUE_DOC_ATTACHED = "public.documentqueue.document-attached";
    private static final UUID userId = randomUUID();

    private static final MessageConsumer documentAttachedEventConsumer = publicEvents.createConsumer(PUBLIC_DOCUMENT_QUEUE_DOC_ATTACHED);
    private static final MessageConsumer attachDocumentRequested = privateEvents.createConsumer("documentqueue.event.attach-document-requested");
    private static final MessageConsumer documentAlreadyAttached = privateEvents.createConsumer("documentqueue.event.document-already-attached");

    @BeforeClass
    public static void init() {
        setupAsAuthorisedUser(userId, "Legal Advisers");
    }

    @Before
    public void setUp() {
    }

    @Test
    public void shouldSuccessfulyAttachDocument() throws Exception {
        // Create CPS document
        final UUID documentId = uploadFile(PDF_MIME_TYPE);
        final Map<String, String> cpsDocumentValues = newReviewDocumentValues("CPS", "OTHER", documentId);
        cpsDocumentValues.put("fileName", PUBLIC_DOCUMENT_REVIEW_REQUIRED);
        newDocumentReviewRequired(cpsDocumentValues);

        postAttachDocument(documentId);

        verifyDocumentAttached(documentId);
        verifyAttachDocumentRequested(documentId);
        verifyDocumentAlreadyAttachedNotRaised();

        // Now send another attach document event
        postAttachDocument(documentId);

        verifyDocumentAlreadyAttached(documentId);
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

    private static void verifyDocumentAttached(UUID documentId) {
        final JsonPath topicMessage = publicEvents.retrieveMessage(documentAttachedEventConsumer);
        final String payloadDocumentId = topicMessage.getString("documentId");
        assertThat(payloadDocumentId, is(documentId.toString()));
    }

    private static void verifyAttachDocumentRequested(UUID documentId) {
        final JsonPath topicMessage = QueueUtil.retrieveMessage(attachDocumentRequested);
        assertThat(topicMessage, notNullValue());

        final String payloadDocumentId = topicMessage.getString("documentId");
        final HashMap<String, Object> payloadCourtDocument = topicMessage.getJsonObject("courtDocument");
        assertThat(payloadDocumentId, is(documentId.toString()));
        assertThat(payloadCourtDocument.get("courtDocumentId"), is(documentId.toString()));
    }

    private static void verifyDocumentAlreadyAttachedNotRaised() {
        final JsonPath topicMessage = QueueUtil.retrieveMessage(documentAlreadyAttached, 100);
        assertThat(topicMessage, nullValue());
    }

    private static void verifyDocumentAlreadyAttached(UUID documentId) {
        final JsonPath topicMessage = QueueUtil.retrieveMessage(documentAlreadyAttached);
        assertThat(topicMessage, notNullValue());

        final String payloadDocumentId = topicMessage.getString("documentId");
        assertThat(payloadDocumentId, is(documentId.toString()));
    }
}
