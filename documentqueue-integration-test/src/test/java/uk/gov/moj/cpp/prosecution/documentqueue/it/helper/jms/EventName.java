package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms;

import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventType.DOCUMENTQUEUE_EVENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventType.PUBLIC_EVENT;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventType.USERSGROUPS_EVENT;

public enum EventName {

    //DOCUMENT QUEUE EVENT NAMES
    DOCUMENTQUEUE_EVENT_OUTSTANDING_DOCUMENT_RECEIVED(DOCUMENTQUEUE_EVENT, "documentqueue.event.outstanding-document-received"),
    DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_IN_PROGRESS(DOCUMENTQUEUE_EVENT, "documentqueue.event.document-marked-inprogress"),
    DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_COMPLETED(DOCUMENTQUEUE_EVENT, "documentqueue.event.document-marked-completed"),
    DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_OUTSTANDING(DOCUMENTQUEUE_EVENT, "documentqueue.event.document-marked-outstanding"),
    DOCUMENTQUEUE_EVENT_DOCUMENT_MARKED_DELETED(DOCUMENTQUEUE_EVENT, "documentqueue.event.document-marked-deleted"),

    //USERSGROUPS EVENT NAMES
    USERSGROUPS_EVENTS_USER_CREATED(USERSGROUPS_EVENT, "usersgroups.user-created"),

    //Public Event
    SCAN_ENVELOPE_REGISTERED(PUBLIC_EVENT, "public.stagingbulkscan.scan-envelope-registered"),
    DOCUMENT_REVIEW_REQUIRED(PUBLIC_EVENT, "public.prosecutioncasefile.document-review-required"),
    DOCUMENT_MARKED_FOLLOW_UP(PUBLIC_EVENT, "public.stagingbulkscan.document-marked-for-follow-up");



    private final String eventName;
    private final String topic;

    EventName(final EventType type, final String eventName) {
        this.topic = type.getStringValue();
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public String getTopicName() {
        return topic;
    }
}
