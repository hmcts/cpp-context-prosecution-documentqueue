package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util;

public class EventPayloadUtil {

    public static final String OUTSTANDING_DOCUMENT_RECEIVED = "/documentqueue/documentqueue.event.outstanding-document-received.json";
    public static final String PUBLIC_SCAN_ENVELOPE_REGISTERED = "/documentqueue/public.stagingbulkscan.scan-envelope-registered.json";
    public static final String PUBLIC_DOCUMENT_REVIEW_REQUIRED = "/documentqueue/public.prosecutioncasefile.document-review-required.json";
    public static final String PUBLIC_DOCUMENT_MARKED_FOLLOW_UP = "/documentqueue/public.stagingbulkscan.document-marked-follow-up.json";
    public static final String DOCUMENT_STATUS_UPDATED = "/documentqueue/documentqueue.event.document-status-updated.json";
    public static final String ATTACH_DOCUMENT = "/documentqueue/documentqueue.attach-document.json";

}
