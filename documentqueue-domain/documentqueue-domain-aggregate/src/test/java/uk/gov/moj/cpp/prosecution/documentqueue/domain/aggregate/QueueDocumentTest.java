package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.DELETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;

import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.moj.cpp.documentqueue.event.DocumentDeleteFromFileStoreRequested;
import uk.gov.moj.cpp.documentqueue.event.DocumentDeletedFromFileStore;
import uk.gov.moj.cpp.documentqueue.event.DocumentMarkedDeleted;
import uk.gov.moj.cpp.platform.test.serializable.AggregateSerializableChecker;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class QueueDocumentTest {

    private AggregateSerializableChecker aggregateSerializableChecker = new AggregateSerializableChecker();

    private QueueDocument queueDocument = new QueueDocument();

    private UUID documentId = UUID.randomUUID();
    private UUID fileServiceId = UUID.randomUUID();

    @Test
    public void shouldCheckAggregatesAreSerializable() {
        aggregateSerializableChecker.checkAggregatesIn("uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate");
    }

    @Test
    public void shouldRaiseDocumentDeleteFromFileStoreRequested() {
        final Document document = Document.document()
                .withScanDocumentId(documentId)
                .withFileServiceId(fileServiceId)
                .withStatus(OUTSTANDING).build();
        queueDocument.receiveOutstandingDocument(document);

        final List<Object> list = Stream.concat(
                queueDocument.updateDocumentStatus(DELETED, true),
                queueDocument.requestDocumentDeleteFromFileStore())
                .collect(Collectors.toList());

        assertThat(list.size(), is(2));
        assertThat(list.get(0), instanceOf(DocumentMarkedDeleted.class));
        assertThat(((DocumentMarkedDeleted) list.get(0)).getDocumentId(), is(documentId));

        assertThat(list.get(1), instanceOf(DocumentDeleteFromFileStoreRequested.class));
        assertThat(((DocumentDeleteFromFileStoreRequested) list.get(1)).getDocumentId(), is(documentId));
    }

    @Test
    public void shouldRaiseDocumentDeletedFromFileStore() {
        final Document document = Document.document()
                .withScanDocumentId(documentId)
                .withFileServiceId(fileServiceId)
                .withStatus(OUTSTANDING).build();
        queueDocument.receiveOutstandingDocument(document);

        final List<Object> list =
                queueDocument
                        .markDocumentDeletedFromFileStore()
                        .collect(Collectors.toList());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), instanceOf(DocumentDeletedFromFileStore.class));
        assertThat(((DocumentDeletedFromFileStore) list.get(0)).getDocumentId(), is(documentId));
    }

}
