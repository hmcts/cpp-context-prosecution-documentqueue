package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequestReceived.deleteExpiredDocumentsRequestReceived;
import static uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequested.deleteExpiredDocumentsRequested;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequestReceived;
import uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class DocumentsExpiration implements Aggregate {

    // all expired documents deletion is tied with this
    public static final UUID STREAM_ID = UUID.fromString("1a47e6d5-27fe-4b00-bc40-2dffbf8602dd");

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DeleteExpiredDocumentsRequestReceived.class).apply(e ->
                        doNothing()
                ),
                when(DeleteExpiredDocumentsRequested.class).apply(e ->
                        doNothing()
                ));
    }

    public Stream<Object> deleteExpiredDocuments() {
        return Stream.builder()
                .add(deleteExpiredDocumentsRequestReceived()
                        .withRequestedAt(ZonedDateTime.now())
                        .build())
                .build();
    }

    public Stream<Object> markDeleteExpiredDocumentsAsRequested(final List<UUID> documentIds) {
        return Stream.builder()
                .add(deleteExpiredDocumentsRequested()
                        .withDocumentIds(documentIds)
                        .build())
                .build();
    }

}
