package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;


import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.COMPLETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.DELETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.model.CourtDocument;
import uk.gov.moj.cpp.DocumentStatusUpdated;
import uk.gov.moj.cpp.documentqueue.event.AttachDocumentRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentAlreadyAttached;
import uk.gov.moj.cpp.documentqueue.event.DocumentAttached;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedCompleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedDeleted;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedInprogress;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedOutstanding;
import uk.gov.moj.cpp.documentqueue.event.DocumentStatusUpdateFailed;
import uk.gov.moj.cpp.documentqueue.event.OutstandingDocumentReceived;

import java.util.UUID;
import java.util.stream.Stream;

public class QueueDocument implements Aggregate {

    private Status documentStatus;

    private UUID documentId;

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

    private Stream<Object> markDocumentDeleted() {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (COMPLETED.equals(documentStatus)) {
            streamBuilder.add(DocumentMarkedDeleted.documentMarkedDeleted().withDocumentId(documentId).build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> receiveAttachDocument(final CourtDocument courtDocument) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (documentAttached) {
            streamBuilder.add(DocumentAlreadyAttached.documentAlreadyAttached().withDocumentId(documentId).build());
        } else if(Source.CPS.equals(source) && !COMPLETED.equals(documentStatus)) {
            streamBuilder.add(AttachDocumentRequested.attachDocumentRequested().withDocumentId(documentId).withCourtDocument(courtDocument).build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> attachDocument(final UUID documentId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
            streamBuilder.add(DocumentAttached.documentAttached().withDocumentId(documentId).build());
        return apply(streamBuilder.build());
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



    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(OutstandingDocumentReceived.class).apply(e -> {
                    this.documentStatus = OUTSTANDING;
                    this.documentId = e.getOutstandingDocument().getScanDocumentId();
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
                ));
    }

    public Stream<Object> updateDocumentStatus(final Status status, final Boolean override ) {
          switch (status) {
              case OUTSTANDING: return markDocumentAsOutstanding();
              case IN_PROGRESS: return  markDocumentAsInProgress();
              case DELETED:   return markDocumentDeleted();
              case COMPLETED: return markDocumentAsCompleted(override);
          }
          return null;
    }

    public Stream<Object> markDocumentAsCompletedForEjectedOrFilteredCase() {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if(null != this.documentId) {
            streamBuilder.add(DocumentMarkedDeleted.documentMarkedDeleted().withDocumentId(documentId).build());
        }
        return apply(streamBuilder.build());
    }
}
