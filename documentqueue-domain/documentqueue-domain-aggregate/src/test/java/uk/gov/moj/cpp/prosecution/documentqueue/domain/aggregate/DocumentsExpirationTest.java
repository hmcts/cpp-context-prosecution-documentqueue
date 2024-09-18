package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequestReceived;
import uk.gov.moj.cpp.documentqueue.event.DeleteExpiredDocumentsRequested;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class DocumentsExpirationTest {

    final DocumentsExpiration documentsExpiration = new DocumentsExpiration();

    @Test
    public void shouldDeleteExpiredDocuments() {
        final List<Object>  eventsList = documentsExpiration
                .deleteExpiredDocuments()
                .collect(Collectors.toList());

        final Object event = eventsList.get(0);
        assertThat(event, instanceOf(DeleteExpiredDocumentsRequestReceived.class));
        assertThat(((DeleteExpiredDocumentsRequestReceived) event)
                .getRequestedAt(), notNullValue());
    }

    @Test
    public void shouldMarkDeleteExpiredDocumentsAsRequested() {
        final UUID documentId = UUID.randomUUID();
        final List<UUID> documentIds = Arrays.asList(documentId);
        final List<Object>  eventsList = documentsExpiration
                .markDeleteExpiredDocumentsAsRequested(documentIds)
                .collect(Collectors.toList());

        final Object event = eventsList.get(0);
        assertThat(event, instanceOf(DeleteExpiredDocumentsRequested.class));
        assertThat(((DeleteExpiredDocumentsRequested) event)
                .getDocumentIds().get(0), equalTo(documentId));
    }

}