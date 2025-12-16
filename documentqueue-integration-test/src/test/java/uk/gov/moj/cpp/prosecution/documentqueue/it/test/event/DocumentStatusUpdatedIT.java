package uk.gov.moj.cpp.prosecution.documentqueue.it.test.event;

import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.documentMarkedCompleted;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.documentMarkedDeleted;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.documentMarkedInProgress;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.documentMarkedOutstanding;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newOutstandingDocumentReceived;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.DOCUMENT_STATUS_UPDATED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.OUTSTANDING_DOCUMENT_RECEIVED;

import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class DocumentStatusUpdatedIT extends BaseIT {

    @Test
    public void shouldHandleDocumentMarkedInProgressEvent() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "PLEA", "OUTSTANDING");
        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
        values.replace("fileName", DOCUMENT_STATUS_UPDATED);
        documentMarkedInProgress(values);
    }

    @Test
    public void shouldHandleDocumentMarkedCompletedEvent() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "PLEA", "OUTSTANDING");
        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
        values.replace("fileName", DOCUMENT_STATUS_UPDATED);
        documentMarkedInProgress(values);
        documentMarkedCompleted(values);
    }

    @Test
    public void shouldHandleDocumentMarkedDeletedEvent() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "PLEA", "OUTSTANDING");
        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
        values.replace("fileName", DOCUMENT_STATUS_UPDATED);
        documentMarkedInProgress(values);
        documentMarkedCompleted(values);
        documentMarkedDeleted(values);
    }

    @Test
    public void shouldHandleDocumentMarkedOutstandingEvent() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "PLEA", "OUTSTANDING");
        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
        values.replace("fileName", DOCUMENT_STATUS_UPDATED);
        documentMarkedInProgress(values);
        documentMarkedOutstanding(values);
    }
}
