package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;


import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Source.CPS;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.COMPLETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.DELETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.FILE_DELETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.moj.cpp.documentqueue.event.DocumentDeleteFromFileStoreRequested.documentDeleteFromFileStoreRequested;
import static uk.gov.moj.cpp.documentqueue.event.DocumentDeletedFromFileStore.documentDeletedFromFileStore;
import static uk.gov.moj.cpp.documentqueue.event.DocumentMarkedDeleted.documentMarkedDeleted;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.model.CourtDocument;
import uk.gov.moj.cpp.DocumentStatusUpdated;
import uk.gov.moj.cpp.documentqueue.event.AttachDocumentRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentAlreadyAttached;
import uk.gov.moj.cpp.documentqueue.event.DocumentAttached;
import uk.gov.moj.cpp.documentqueue.event.DocumentDeleteFromFileStoreRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentDeletedFromFileStore;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedCompleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedDeleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedFileDeleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedInprogress;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedOutstanding;
import uk.gov.moj.cpp.documentqueue.event.DocumentStatusUpdateFailed;
import uk.gov.moj.cpp.documentqueue.event.OutstandingDocumentReceived;

import java.util.UUID;
import java.util.stream.Stream;

public class QueueDocument implements Aggregate {

    private static final long serialVersionUID = 1l;

    private Status documentStatus;

    private UUID documentId;

    private UUID fileServiceId;

    private Source source;

    private boolean documentAttached;

    private static final String DOCUMENT_STATUS_ALREADY_UPDATED = "DOC_ALREADY_UPDATED";


    public Stream<Object> receiveOutstandingDocument(final Document document) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (documentStatus == null && document.getStatus().equals(OUTSTANDING)) {
            streamBuilder.add(OutstandingDocumentReceived.outstandingDocumentReceived().withOutstandingDocument(document).build());
        }
        return apply(streamBuilder.build());
    }

    private Stream<Object> markDocumentAsOutstanding() {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (documentStatus.equals(IN_PROGRESS)) {
            streamBuilder.add(DocumentMarkedOutstanding.documentMarkedOutstanding().withDocumentId(documentId).build());
            streamBuilder.add(DocumentStatusUpdated.documentStatusUpdated().withDocumentId(documentId).withStatus(OUTSTANDING).build());
        } else {
            streamBuilder.add(DocumentStatusUpdateFailed.documentStatusUpdateFailed().withDocumentId(documentId).withErrorDescription(DOCUMENT_STATUS_ALREADY_UPDATED).withStatus(documentStatus.toString()).build());
        }
        return apply(streamBuilder.build());
    }

    private Stream<Object> markDocumentAsInProgress() {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (documentStatus.equals(OUTSTANDING)) {
            streamBuilder.add(DocumentMarkedInprogress.documentMarkedInprogress().withDocumentId(documentId).build());
            streamBuilder.add(DocumentStatusUpdated.documentStatusUpdated().withDocumentId(documentId).withStatus(IN_PROGRESS).build());
        } else {
            streamBuilder.add(DocumentStatusUpdateFailed.documentStatusUpdateFailed().withDocumentId(documentId).withErrorDescription(DOCUMENT_STATUS_ALREADY_UPDATED).withStatus(documentStatus.toString()).build());
        }
        return apply(streamBuilder.build());
    }

    private Stream<Object> markDocumentDeleted(final Boolean override) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if ((Boolean.TRUE.equals(override) && !DELETED.equals(documentStatus))
                || COMPLETED.equals(documentStatus)) {
            streamBuilder.add(documentMarkedDeleted().withDocumentId(documentId).build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> receiveAttachDocument(final CourtDocument courtDocument) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (documentAttached) {
            streamBuilder.add(DocumentAlreadyAttached.documentAlreadyAttached().withDocumentId(documentId).build());
        } else if(CPS.equals(source) && !COMPLETED.equals(documentStatus)) {
            streamBuilder.add(AttachDocumentRequested.attachDocumentRequested().withDocumentId(documentId).withCourtDocument(courtDocument).build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> attachDocument(final UUID documentId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
            streamBuilder.add(DocumentAttached.documentAttached().withDocumentId(documentId).build());
        return apply(streamBuilder.build());
    }

    public Stream<Object> removeDocumentFromQueue() {
        if (null != documentId && CPS.equals(source)) {
            return updateDocumentStatus(COMPLETED, true);
        }
        return Stream.empty();
    }

    private Stream<Object> markDocumentAsCompleted(final Boolean override) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if ((Boolean.TRUE.equals(override) && OUTSTANDING.equals(documentStatus)) || IN_PROGRESS.equals(documentStatus)) {
            streamBuilder.add(DocumentMarkedCompleted.documentMarkedCompleted().withDocumentId(documentId).build());
            streamBuilder.add(DocumentStatusUpdated.documentStatusUpdated().withDocumentId(documentId).withStatus(COMPLETED).build());
        } else {
            streamBuilder.add(DocumentStatusUpdateFailed.documentStatusUpdateFailed().withDocumentId(documentId).withErrorDescription(DOCUMENT_STATUS_ALREADY_UPDATED).withStatus(documentStatus.toString()).build());
        }
        return apply(streamBuilder.build());
    }

    private Stream<Object> markDocumentAsFileDeleted(final Boolean override) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if ((Boolean.TRUE.equals(override)) || COMPLETED.equals(documentStatus) || DELETED.equals(documentStatus)) {
            streamBuilder.add(DocumentMarkedFileDeleted.documentMarkedFileDeleted().withDocumentId(documentId).build());
            streamBuilder.add(DocumentStatusUpdated.documentStatusUpdated().withDocumentId(documentId).withStatus(FILE_DELETED).build());
        } else {
            streamBuilder.add(DocumentStatusUpdateFailed.documentStatusUpdateFailed().withDocumentId(documentId).withStatus(documentStatus.toString()).build());
        }
        return apply(streamBuilder.build());
    }

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(OutstandingDocumentReceived.class).apply(e -> {
                    this.documentStatus = OUTSTANDING;
                    this.documentId = e.getOutstandingDocument().getScanDocumentId();
                    this.fileServiceId = e.getOutstandingDocument().getFileServiceId();
                    this.source= e.getOutstandingDocument().getSource();
                }),
                when(DocumentMarkedCompleted.class).apply(e ->
                    this.documentStatus = COMPLETED
                ),
                when(DocumentMarkedInprogress.class).apply(e ->
                    this.documentStatus = IN_PROGRESS
                ),
                when(DocumentMarkedOutstanding.class).apply(e ->
                    this.documentStatus = OUTSTANDING
                ),
                when(DocumentStatusUpdated.class).apply(e ->
                    doNothing()
                ),
                when(DocumentMarkedDeleted.class).apply(e ->
                    this.documentStatus = DELETED
                ),
                when(DocumentStatusUpdateFailed.class).apply(e ->
                    doNothing()
                ),
                when(AttachDocumentRequested.class).apply(e ->
                    doNothing()
                ),
                when(DocumentAttached.class).apply(e ->
                    this.documentAttached = true
                ),
                when(DocumentAlreadyAttached.class).apply(e ->
                        doNothing()
                ),
                when(DocumentDeleteFromFileStoreRequested.class).apply(e ->
                        doNothing()
                ),
                when(DocumentDeletedFromFileStore.class).apply(e ->
                        doNothing()
                ),
                when(DocumentMarkedFileDeleted.class).apply(e ->
                        doNothing()
                ), otherwiseDoNothing());
    }

    public Stream<Object> updateDocumentStatus(final Status status, final Boolean override ) {
          switch (status) {
              case OUTSTANDING: return markDocumentAsOutstanding();
              case IN_PROGRESS: return  markDocumentAsInProgress();
              case DELETED:   return markDocumentDeleted(override);
              case COMPLETED: return markDocumentAsCompleted(override);
              case FILE_DELETED: return markDocumentAsFileDeleted(override);
          }
          return null;
    }

    public Stream<Object> markDocumentAsCompletedForEjectedOrFilteredCase() {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if(null != this.documentId) {
            streamBuilder.add(documentMarkedDeleted().withDocumentId(documentId).build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> requestDocumentDeleteFromFileStore() {
        if (fileServiceId != null) {
            final Stream.Builder<Object> streamBuilder = Stream.builder();
            return streamBuilder.add(
                    documentDeleteFromFileStoreRequested()
                            .withFileServiceId(fileServiceId)
                            .withDocumentId(documentId)
                            .build()).build();
        }
        return Stream.empty();
    }

    public Stream<Object> markDocumentDeletedFromFileStore() {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        return streamBuilder.add(
                documentDeletedFromFileStore()
                        .withFileServiceId(fileServiceId)
                        .withDocumentId(documentId)
                        .build())
                .build();
    }


    public UUID getFileServiceId() {
        return fileServiceId;
    }

}
