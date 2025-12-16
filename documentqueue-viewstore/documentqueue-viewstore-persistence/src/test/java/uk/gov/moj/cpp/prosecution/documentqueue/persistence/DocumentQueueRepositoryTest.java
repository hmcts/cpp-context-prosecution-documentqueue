package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(CdiTestRunner.class)
public class DocumentQueueRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private DocumentQueueRepository documentQueueRepository;

    @Test
    public void shouldGetDocumentsByStatusAndSource() {

        final UUID document1Id = randomUUID();
        final UUID document2Id = randomUUID();

        final Document document1 = createDocument(document1Id, Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().minusDays(1));
        documentQueueRepository.save(document1);
        final Document document2 = createDocument(document2Id, Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document2);

        List<Document> result = documentQueueRepository.getDocumentList(Optional.of(Source.BULKSCAN), Optional.of(Status.COMPLETED),"statusUpdatedDate", "desc", 0, 50).getRight();

        assertThat(result.size(), is(2));


    }

    @Test
    public void shouldGetDocumentsBySource() {

        final UUID document1Id = randomUUID();
        final UUID document2Id = randomUUID();
        final UUID document3Id = randomUUID();
        final UUID document4Id = randomUUID();
        final UUID document5Id = randomUUID();

        final Document document1 = createDocument(document1Id, Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().minusDays(1));
        documentQueueRepository.save(document1);
        final Document document2 = createDocument(document2Id, Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document2);
        final Document document3 = createDocument(document3Id, Status.DELETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document3);
        final Document document4 = createDocument(document4Id, Status.FILE_DELETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document4);
        final Document document5= createDocument(document5Id, Status.FILE_DELETED, Source.CPS, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document5);

        List<Document> result = documentQueueRepository.getDocumentList(Optional.of(Source.BULKSCAN), Optional.empty(),"statusUpdatedDate", "desc", 0, 50).getRight();

        assertThat(result.size(), is(2));


    }

    @Test
    public void shouldGetDocumentsWithOutSourceAndStatus() {

        final UUID document1Id = randomUUID();
        final UUID document2Id = randomUUID();
        final UUID document3Id = randomUUID();
        final UUID document4Id = randomUUID();

        final Document document1 = createDocument(document1Id, Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().minusDays(1));
        documentQueueRepository.save(document1);
        final Document document2 = createDocument(document2Id, Status.IN_PROGRESS, Source.CPS, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document2);
        final Document document3 = createDocument(document3Id, Status.DELETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document3);
        final Document document4 = createDocument(document4Id, Status.FILE_DELETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document4);

        List<Document> result = documentQueueRepository.getDocumentList(Optional.empty(), Optional.empty(),"statusUpdatedDate", "desc", 0, 50).getRight();

        assertThat(result.size(), is(2));


    }

    @Test
    public void shouldGetDocumentsByStatus() {

        final UUID document1Id = randomUUID();
        final UUID document2Id = randomUUID();

        final Document document1 = createDocument(document1Id, Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().minusDays(1));
        documentQueueRepository.save(document1);
        final Document document2 = createDocument(document2Id, Status.COMPLETED, Source.BULKSCAN, ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        documentQueueRepository.save(document2);

        List<Document> result = documentQueueRepository.getDocumentList(Optional.empty(), Optional.of(Status.COMPLETED),"vendorReceivedDate", "desc", 0, 50).getRight();

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
