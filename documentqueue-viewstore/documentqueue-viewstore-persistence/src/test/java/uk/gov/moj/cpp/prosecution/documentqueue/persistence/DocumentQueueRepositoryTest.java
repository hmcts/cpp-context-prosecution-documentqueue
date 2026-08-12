package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DocumentQueueRepositoryTest {

    private static final String PERSISTENCE_UNIT = "documentqueue-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DocumentQueueRepository documentQueueRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        documentQueueRepository = new DocumentQueueRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(documentQueueRepository);
    }

    @Test
    public void shouldGetDocumentsByStatusAndSource() {

        final Document document1 = createDocument(randomUUID(), Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().minusDays(1));
        documentQueueRepository.save(document1);
        final Document document2 = createDocument(randomUUID(), Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document2);

        final List<Document> result = documentQueueRepository.getDocumentList(Optional.of(Source.BULKSCAN), Optional.of(Status.COMPLETED), "statusUpdatedDate", "desc", 0, 50).getRight();

        assertThat(result.size(), is(2));
    }

    @Test
    public void shouldGetDocumentsBySource() {

        documentQueueRepository.save(createDocument(randomUUID(), Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().minusDays(1)));
        documentQueueRepository.save(createDocument(randomUUID(), Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2)));
        documentQueueRepository.save(createDocument(randomUUID(), Status.DELETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2)));
        documentQueueRepository.save(createDocument(randomUUID(), Status.FILE_DELETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2)));
        documentQueueRepository.save(createDocument(randomUUID(), Status.FILE_DELETED, Source.CPS, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2)));

        final List<Document> result = documentQueueRepository.getDocumentList(Optional.of(Source.BULKSCAN), Optional.empty(), "statusUpdatedDate", "desc", 0, 50).getRight();

        assertThat(result.size(), is(2));
    }

    @Test
    public void shouldGetDocumentsWithOutSourceAndStatus() {

        documentQueueRepository.save(createDocument(randomUUID(), Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().minusDays(1)));
        documentQueueRepository.save(createDocument(randomUUID(), Status.IN_PROGRESS, Source.CPS, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2)));
        documentQueueRepository.save(createDocument(randomUUID(), Status.DELETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2)));
        documentQueueRepository.save(createDocument(randomUUID(), Status.FILE_DELETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2)));

        final List<Document> result = documentQueueRepository.getDocumentList(Optional.empty(), Optional.empty(), "statusUpdatedDate", "desc", 0, 50).getRight();

        assertThat(result.size(), is(2));
    }

    @Test
    public void shouldGetDocumentsByStatus() {

        documentQueueRepository.save(createDocument(randomUUID(), Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().minusDays(1)));
        documentQueueRepository.save(createDocument(randomUUID(), Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2)));

        final List<Document> result = documentQueueRepository.getDocumentList(Optional.empty(), Optional.of(Status.COMPLETED), "vendorReceivedDate", "desc", 0, 50).getRight();

        assertThat(result.size(), is(2));
    }

    private Document createDocument(final UUID documentId, final Status status, final Source source, final ZonedDateTime receivedDate, final ZonedDateTime statusUpdatedDate) {
        return Document.DocumentBuilder.document()
                .withId(documentId)
                .withCaseId(randomUUID())
                .withStatus(status)
                .withSource(source)
                .withReceivedDateTime(receivedDate)
                .withStatusUpdatedDate(statusUpdatedDate)
                .build();
    }
}
