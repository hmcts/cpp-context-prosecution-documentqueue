package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.COMPLETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.DELETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.DocumentQueueTableList.DOCUMENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.waitUntilDataPersist;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_COMPLETED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_DELETED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_IN_PROGRESS;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_OUTSTANDING;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.DOCUMENTQUEUE_EVENT_OUTSTANDING_DOCUMENT_RECEIVED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.DOCUMENT_MARKED_FOLLOW_UP;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.SCAN_ENVELOPE_REGISTERED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.SimpleMessageQueueClient.publicEventToTopic;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.SimpleMessageQueueClient.publishPrivateEvent;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DocumentFactory {

    private static final String DOCUMENT_CRITERIA = "id = '%s'";
    private static final String CPS_DOCUMENT_CRITERIA ="file_service_id = '%s'";
    private static final String DOCUMENT_STATUS_CRITERIA ="id = '%s' and status = '%s'";

    public static void newOutstandingDocumentReceived(final Map<String, String> values) {
        final String fileName = values.get("fileName");
        final String documentId = values.get("documentId");
        publishPrivateEvent(DOCUMENTQUEUE_EVENT_OUTSTANDING_DOCUMENT_RECEIVED, fileName, values);
        waitUntilDataPersist(DOCUMENT.name(), format(DOCUMENT_CRITERIA, documentId), 1);
    }

    public static void newScanEnvelopeRegistered(final Map<String, String> values) {
        final String fileName = values.get("fileName");
        final String documentId = values.get("documentId");
        publicEventToTopic(SCAN_ENVELOPE_REGISTERED.getEventName(), SCAN_ENVELOPE_REGISTERED.getTopicName(), fileName, values, ZonedDateTime.now());
        waitUntilDataPersist(DOCUMENT.name(), format(DOCUMENT_CRITERIA, documentId), 1);
    }

    public static void newDocumentMarkedAsFollowUp(final Map<String, String> values) {
        final String fileName = values.get("fileName");
        final String documentId = values.get("documentId");
        publicEventToTopic(DOCUMENT_MARKED_FOLLOW_UP.getEventName(), DOCUMENT_MARKED_FOLLOW_UP.getTopicName(), fileName, values, ZonedDateTime.now());
        waitUntilDataPersist(DOCUMENT.name(), format(DOCUMENT_CRITERIA, documentId), 1);
    }

    public static void newDocumentReviewRequired(final Map<String,String> values) {
        final String fileStoreId  = values.get("fileStoreId");
        final String fileName = values.get("fileName");
        publicEventToTopic(DOCUMENT_REVIEW_REQUIRED.getEventName(), DOCUMENT_REVIEW_REQUIRED.getTopicName(), fileName, values, ZonedDateTime.now());
        waitUntilDataPersist(DOCUMENT.name(), format(CPS_DOCUMENT_CRITERIA,fileStoreId), 1);
    }

    public static void documentMarkedInProgress(final Map<String, String> values) {
        final String fileName = values.get("fileName");
        final String documentId = values.get("documentId");
        publishPrivateEvent(DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_IN_PROGRESS, fileName, values);
        waitUntilDataPersist(DOCUMENT.name(), format(DOCUMENT_STATUS_CRITERIA, documentId, IN_PROGRESS.toString()), 1);
    }

    public static void documentMarkedCompleted(final Map<String, String> values) {
        final String fileName = values.get("fileName");
        final String documentId = values.get("documentId");
        publishPrivateEvent(DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_COMPLETED, fileName, values);
        waitUntilDataPersist(DOCUMENT.name(), format(DOCUMENT_STATUS_CRITERIA, documentId, COMPLETED.toString()), 1);
    }

    public static void documentMarkedDeleted(final Map<String, String> values) {
        final String fileName = values.get("fileName");
        final String documentId = values.get("documentId");
        publishPrivateEvent(DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_DELETED, fileName, values);
        waitUntilDataPersist(DOCUMENT.name(), format(DOCUMENT_STATUS_CRITERIA, documentId, DELETED.toString()), 1);
    }

    public static void documentMarkedOutstanding(final Map<String, String> values) {
        final String fileName = values.get("fileName");
        final String documentId = values.get("documentId");
        publishPrivateEvent(DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_OUTSTANDING, fileName, values);
        waitUntilDataPersist(DOCUMENT.name(), format(DOCUMENT_STATUS_CRITERIA, documentId, OUTSTANDING.toString()), 1);
    }

    public static Map<String, String> newDocumentValues(final String source, final String type, final String status) {
        return newDocumentValues(randomUUID(), source, type, status, randomUUID(), randomUUID());
    }

    public static Map<String, String> newDocumentValues(final UUID documentId,
                                                        final String source,
                                                        final String type,
                                                        final String status,
                                                        final UUID materialId,
                                                        final UUID fileServiceId) {
        final Map<String, String> valueMap = new HashMap<>();
        valueMap.put("documentId", documentId.toString());
        valueMap.put("source", source);
        valueMap.put("type", type);
        valueMap.put("status", status);
        valueMap.put("materialId", materialId.toString());
        valueMap.put("fileServiceId", fileServiceId.toString());
        valueMap.put("receivedDateTime", "2020-05-21T20:11:32.013Z");
        return valueMap;
    }

    public static Map<String,String> newReviewDocumentValues(final String source, final String type,final UUID fileStoreId) {
        final Map<String,String> values = new HashMap<>();
        values.put("source",source);
        values.put("documentType",type);
        values.put("fileStoreId",fileStoreId.toString());
        values.put("cmsDocumentId",UUID.randomUUID().toString());
        return values;
    }
}
