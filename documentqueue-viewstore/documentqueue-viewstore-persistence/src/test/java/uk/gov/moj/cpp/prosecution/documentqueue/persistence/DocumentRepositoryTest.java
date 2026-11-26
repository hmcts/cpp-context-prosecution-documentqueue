package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.prosecution.documentqueue.entity.Document.DocumentBuilder.document;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DocumentRepositoryTest extends BaseTransactionalJunit4Test  {

    @Inject
    private DocumentRepository documentRepository;

    @Test
    public void shouldSaveAndFindDocumentById() throws Exception {

        final Document document_1 = aDocument(1);
        final Document document_2 = aDocument(2);
        final Document document_3 = aDocument(3);

        documentRepository.save(document_1);
        documentRepository.save(document_2);
        documentRepository.save(document_3);

        assertThat(documentRepository.findBy(document_1.getScanDocumentId()), is(document_1));
        assertThat(documentRepository.findBy(document_2.getScanDocumentId()), is(document_2));
        assertThat(documentRepository.findBy(document_3.getScanDocumentId()), is(document_3));
    }

    @Test
    public void shouldSaveAndFindDocumentBySource() throws Exception {

        final Document document_1 = aDocument(1);
        final Document document_2 = aDocument(2);
        final Document document_3 = aDocument(3);
        final Document document_4 = aDocument(1);

        documentRepository.save(document_1);
        documentRepository.save(document_2);
        documentRepository.save(document_3);
        documentRepository.save(document_4);

        final List<Document> documents_1 = documentRepository.findBySourceAndStatusNotInOrderByVendorReceivedDateAsc(document_2.getSource(), Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_1.size(), is(1));
        assertThat(documents_1.get(0), is(document_2));

        final List<Document> documents_2 = documentRepository.findBySourceAndStatusNotInOrderByVendorReceivedDateAsc(document_3.getSource(), Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_2.size(), is(0));

        final List<Document> documents_3 = documentRepository.findBySourceAndStatusNotInOrderByVendorReceivedDateAsc(document_4.getSource(), Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_3.size(), is(2));

        final Document document_resp_1 = documents_3.get(0);
        final Document document_resp_2 = documents_3.get(1);

        assertThat(document_resp_2.getVendorReceivedDate(), anyOf(greaterThan(document_resp_1.getVendorReceivedDate()), equalTo(document_resp_1.getVendorReceivedDate())));
    }

    @Test
    public void shouldSaveAndFindDocumentByType() throws Exception {

        final Document document_1 = aDocument(1);
        final Document document_2 = aDocument(2);
        final Document document_3 = aDocument(3);
        final Document document_4 = aDocument(1);

        documentRepository.save(document_1);
        documentRepository.save(document_2);
        documentRepository.save(document_3);
        documentRepository.save(document_4);

        final List<Document> documents_1 = documentRepository.findByTypeAndStatusNotInOrderByVendorReceivedDateAsc(document_2.getType(), Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_1.size(), is(1));
        assertThat(documents_1.get(0), is(document_2));

        final List<Document> documents_2 = documentRepository.findByTypeAndStatusNotInOrderByVendorReceivedDateAsc(document_3.getType(),Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_2.size(), is(0));

        final List<Document> documents_3 = documentRepository.findByTypeAndStatusNotInOrderByVendorReceivedDateAsc(document_4.getType(), Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_3.size(), is(2));

        final Document document_resp_1 = documents_3.get(0);
        final Document document_resp_2 = documents_3.get(1);

        assertThat(document_resp_2.getVendorReceivedDate(), anyOf(greaterThan(document_resp_1.getVendorReceivedDate()), equalTo(document_resp_1.getVendorReceivedDate())));
    }

    @Test
    public void shouldSaveAndFindDocumentByStatus() throws Exception {

        final Document document_1 = aDocument(1);
        final Document document_2 = aDocument(2);
        final Document document_3 = aDocument(3);
        final Document document_4 = aDocument(3);

        documentRepository.save(document_1);
        documentRepository.save(document_2);
        documentRepository.save(document_3);
        documentRepository.save(document_4);

        final List<Document> documents_1 = documentRepository.findByStatusOrderByVendorReceivedDateAsc(document_2.getStatus());
        assertThat(documents_1.size(), is(1));
        assertThat(documents_1.get(0), is(document_2));

        final List<Document> documents_2 = documentRepository.findByStatusOrderByVendorReceivedDateAsc(document_3.getStatus());
        assertThat(documents_2.size(), is(2));

        final Document document_resp_1 = documents_2.get(0);
        final Document document_resp_2 = documents_2.get(1);

        assertThat(document_resp_2.getVendorReceivedDate(), anyOf(greaterThan(document_resp_1.getVendorReceivedDate()), equalTo(document_resp_1.getVendorReceivedDate())));
    }

    @Test
    public void shouldSaveAndFindDocumentBySourceAndType() throws Exception {

        final Document document_1 = aDocument(1);
        final Document document_2 = aDocument(2);
        final Document document_3 = aDocument(3);
        final Document document_4 = aDocument(1);

        documentRepository.save(document_1);
        documentRepository.save(document_2);
        documentRepository.save(document_3);
        documentRepository.save(document_4);

        final List<Document> documents_1 = documentRepository.findBySourceAndTypeAndStatusNotInOrderByVendorReceivedDateAsc(document_2.getSource(), document_2.getType(), Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_1.size(), is(1));
        assertThat(documents_1.get(0), is(document_2));

        final List<Document> documents_2 = documentRepository.findBySourceAndTypeAndStatusNotInOrderByVendorReceivedDateAsc(document_3.getSource(), document_3.getType(), Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_2.size(), is(0));

        final List<Document> documents_3 = documentRepository.findBySourceAndTypeAndStatusNotInOrderByVendorReceivedDateAsc(document_4.getSource(), document_4.getType(), Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_3.size(), is(2));

        final Document document_resp_1 = documents_3.get(0);
        final Document document_resp_2 = documents_3.get(1);

        assertThat(document_resp_2.getVendorReceivedDate(), anyOf(greaterThan(document_resp_1.getVendorReceivedDate()), equalTo(document_resp_1.getVendorReceivedDate())));
    }

    @Test
    public void shouldSaveAndFindDocumentBySourceAndStatus() throws Exception {

        final Document document_1 = aDocument(1);
        final Document document_2 = aDocument(2);
        final Document document_3 = aDocument(3);
        final Document document_4 = aDocument(1);

        documentRepository.save(document_1);
        documentRepository.save(document_2);
        documentRepository.save(document_3);
        documentRepository.save(document_4);

        final List<Document> documents_1 = documentRepository.findBySourceAndStatusOrderByVendorReceivedDateAsc(document_2.getSource(), document_2.getStatus());
        assertThat(documents_1.size(), is(1));
        assertThat(documents_1.get(0), is(document_2));

        final List<Document> documents_2 = documentRepository.findBySourceAndStatusOrderByVendorReceivedDateAsc(document_4.getSource(), document_4.getStatus());
        assertThat(documents_2.size(), is(2));

        final Document document_resp_1 = documents_2.get(0);
        final Document document_resp_2 = documents_2.get(1);

        assertThat(document_resp_2.getVendorReceivedDate(), anyOf(greaterThan(document_resp_1.getVendorReceivedDate()), equalTo(document_resp_1.getVendorReceivedDate())));
    }

    @Test
    public void shouldSaveAndFindAllDocumentsExceptDeleted() throws Exception {

        final Document document_1 = aDocument(1);
        final Document document_2 = aDocument(2);
        final Document document_3 = aDocument(3);
        final Document document_4 = aDocument(0);

        documentRepository.save(document_1);
        documentRepository.save(document_2);
        documentRepository.save(document_3);
        documentRepository.save(document_4);

        final List<Document> documents_1 = documentRepository.findByStatusNotInOrderByVendorReceivedDateAsc(Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_1.size(), is(3));

        final Document document_resp_1 = documents_1.get(0);
        final Document document_resp_2 = documents_1.get(1);
        final Document document_resp_3 = documents_1.get(2);

        assertTrue(!document_resp_1.getVendorReceivedDate().isAfter(document_resp_2.getVendorReceivedDate()));
        assertTrue(!document_resp_2.getVendorReceivedDate().isAfter(document_resp_3.getVendorReceivedDate()));
    }

    private Document aDocument(final int seed) {

        final ZonedDateTime now = new UtcClock().now().plusMinutes(seed);

        final Source[] sources = Source.values();
        final Type[] types = Type.values();
        final Status[] statuses = Status.values();

        return document()
                .withId(randomUUID())
                .withActionedBy(randomUUID())
                .withCasePTIUrn("case PTI urn " + seed)
                .withCaseUrn("case urn " + seed)
                .withDocumentControlNumber("documentControlNumber " + seed)
                .withDocumentName("documentName " + seed)
                .withFileName("file name " + seed)
                .withManualIntervention("manualIntervention " + seed)
                .withNotes("notes " + seed)
                .withProsecutorAuthorityCode("prosecuting authority code " + seed)
                .withProsecutorAuthorityId("prosecutorAuthorityId " + seed)
                .withScanningDate(now.minusMonths(2))
                .withVendorReceivedDate(now.minusMonths(2))
                .withStatusUpdatedDate(now.minusHours(3))
                .withSource(sources[seed % sources.length])
                .withType(types[seed % types.length])
                .withStatus(statuses[seed % statuses.length])
                .build();
    }
}
