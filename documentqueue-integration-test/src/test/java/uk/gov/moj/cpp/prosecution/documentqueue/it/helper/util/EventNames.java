package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util;

public class EventNames {

    public static final String ATTACH_DOCUMENT_REQUESTED = "documentqueue.event.attach-document-requested";
    public static final String DOCUMENT_ALREADY_ATTACHED = "documentqueue.event.document-already-attached";
    public static final String DOCUMENT_MARKED_DELETED = "documentqueue.event.document-marked-deleted";
    public static final String DOCUMENT_DELETED_FROM_FILE_STORE = "documentqueue.event.document-deleted-from-file-store";
    public static final String DELETE_DOCUMENTS_OF_CASES_REQUESTED = "documentqueue.event.delete-documents-of-cases-requested";
    public static final String DELETE_DOCUMENTS_OF_CASES_ACTIONED = "documentqueue.event.delete-documents-of-cases-actioned";
    public static final String DELETE_EXPIRED_DOCUMENTS_RECEIVED = "documentqueue.event.delete-expired-documents-request-received";
    public static final String DELETE_EXPIRED_DOCUMENTS_REQUESTED = "documentqueue.event.delete-expired-documents-requested";
    public static final String DOCUMENT_MARKED_COMPLETED = "documentqueue.event.document-marked-completed";
    public static final String DOCUMENT_MARKED_FILE_DELETED = "documentqueue.event.document-marked-file-deleted";
    public static final String DOCUMENT_DOCUMENT_LINKED_TO_CASE = "documentqueue.event.document-linked-to-case";
    public static final String DOCUMENT_OUT_STANDING_DOCUMENT_RECEIVED = "documentqueue.event.outstanding-document-received";
    public static final String DOCUMENT_DELETE_FROM_FILE_STORE_REQUESTED = "documentqueue.event.document-delete-from-file-store-requested";
    public static final String CASE_MARKED_FILTERED = "documentqueue.event.case-marked-filtered";
    public static final String CASE_MARKED_SUBMISSION_SUCCEEDED = "documentqueue.event.case-marked-submission-succeeded";
    public static final String DOCUMENT_STATUS_UPDATED = "documentqueue.event.document-status-updated";

    public static final String PUBLIC_DOCUMENT_ATTACHED = "public.documentqueue.document-attached";
    public static final String PUBLIC_DOCUMENT_STATUS_UPDATED = "public.documentqueue.document-status-updated";
    public static final String PUBLIC_DOCUMENT_STATUS_UPDATE_FAILED ="public.documentqueue.event.document-status-update-failed";
    public static final String PUBLIC_CASE_OR_APPLICATION_EJECTED ="public.progression.events.case-or-application-ejected";
    public static final String PUBLIC_CASE_OR_APPLICATION_FILTERED = "public.stagingprosecutorsspi.event.prosecution-case-filtered";
    public static final String PUBLIC_CASE_OR_APPLICATION_COMPLETED = "public.prosecutioncasefile.prosecution-submission-succeeded";
    public static final String PUBLIC_CASE_OR_APPLICATION_COMPLETED_WITH_WARNINGS = "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings";
    public static final String PUBLIC_PROSECUTION_CASE_FILE_MATERIAL_ADDED = "public.prosecutioncasefile.material-added";

    public static final String CONTEXT_NAME = "documentqueue";
}
