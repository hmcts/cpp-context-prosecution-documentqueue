package uk.gov.moj.cpp.prosecution.documentqueue.event.listener;

import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.COMPLETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.DELETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedCompleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedDeleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedInprogress;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedOutstanding;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.event.service.DocumentService;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DocumentStatusUpdatedListener {

    @Inject
    private DocumentService documentService;

    @Handles("documentqueue.event.document-marked-deleted")
    public void handleDocumentMarkedAsDeleted(final Envelope<DocumentMarkedDeleted> envelope) {
        final UUID documentId = envelope.payload().getDocumentId();
        final Optional<ZonedDateTime> eventDateTime = envelope.metadata().createdAt();
        updateDocumentStatus(DELETED, documentId, eventDateTime);
    }

    @Handles("documentqueue.event.document-marked-inprogress")
    public void handleDocumentMarkedInProgress(final Envelope<DocumentMarkedInprogress> envelope) {
        final UUID documentId = envelope.payload().getDocumentId();
        final Optional<ZonedDateTime> eventDateTime = envelope.metadata().createdAt();
        updateDocumentStatus(IN_PROGRESS, documentId, eventDateTime);
    }

    @Handles("documentqueue.event.document-marked-outstanding")
    public void handleDocumentMarkedOutstanding(final Envelope<DocumentMarkedOutstanding> envelope) {
        final UUID documentId = envelope.payload().getDocumentId();
        final Optional<ZonedDateTime> eventDateTime = envelope.metadata().createdAt();
        updateDocumentStatus(OUTSTANDING, documentId, eventDateTime);
    }

    @Handles("documentqueue.event.document-marked-completed")
    public void handleDocumentMarkedCompleted(final Envelope<DocumentMarkedCompleted> envelope) {
        final UUID documentId = envelope.payload().getDocumentId();
        final Optional<ZonedDateTime> eventDateTime = envelope.metadata().createdAt();
        updateDocumentStatus(COMPLETED, documentId, eventDateTime);
    }

    public void updateDocumentStatus(final Status status, final UUID documentId, final Optional<ZonedDateTime> eventDateTime) {
        final Document.DocumentBuilder documentBuilder = new Document.DocumentBuilder()
                .withDocument(documentService.getDocumentByDocumentId(documentId));
        documentBuilder.withStatus(status);
        eventDateTime.ifPresent(now -> {
            documentBuilder.withStatusUpdatedDate(now);
            documentBuilder.withLastModified(now);
        });
        documentService.saveDocument(documentBuilder.build());
    }
}
