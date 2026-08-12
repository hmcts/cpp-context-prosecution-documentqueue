package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.prosecution.documentqueue.entity.Document.DocumentBuilder.document;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.mapping.DocumentCountMapping;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DocumentRepositoryTest {

    private static final String PERSISTENCE_UNIT = "documentqueue-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DocumentRepository documentRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        documentRepository = new DocumentRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(documentRepository);
    }

    @Test
    public void shouldSaveAndFindDocumentById() {

        final Document document_1 = documentRepository.save(aDocument(1));
        final Document document_2 = documentRepository.save(aDocument(2));
        final Document document_3 = documentRepository.save(aDocument(3));

        assertThat(documentRepository.findBy(document_1.getScanDocumentId()), is(document_1));
        assertThat(documentRepository.findBy(document_2.getScanDocumentId()), is(document_2));
        assertThat(documentRepository.findBy(document_3.getScanDocumentId()), is(document_3));
    }

    @Test
    public void shouldSaveAndFindDocumentBySource() {

        final Document document_2 = documentRepository.save(aDocument(2));
        final Document document_3 = documentRepository.save(aDocument(3));
        documentRepository.save(aDocument(1));
        final Document document_4 = documentRepository.save(aDocument(1));

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
    public void shouldSaveAndFindDocumentByType() {

        final Document document_2 = documentRepository.save(aDocument(2));
        final Document document_3 = documentRepository.save(aDocument(3));
        documentRepository.save(aDocument(1));
        final Document document_4 = documentRepository.save(aDocument(1));

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
    public void shouldSaveAndFindDocumentByStatus() {

        final Document document_2 = documentRepository.save(aDocument(2));
        final Document document_3 = documentRepository.save(aDocument(3));
        final Document document_4 = documentRepository.save(aDocument(3));
        documentRepository.save(aDocument(1));

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
    public void shouldSaveAndFindDocumentBySourceAndType() {

        final Document document_2 = documentRepository.save(aDocument(2));
        final Document document_3 = documentRepository.save(aDocument(3));
        documentRepository.save(aDocument(1));
        final Document document_4 = documentRepository.save(aDocument(1));

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
    public void shouldSaveAndFindDocumentBySourceAndStatus() {

        final Document document_2 = documentRepository.save(aDocument(2));
        documentRepository.save(aDocument(3));
        documentRepository.save(aDocument(1));
        final Document document_4 = documentRepository.save(aDocument(1));

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
    public void shouldSaveAndFindAllDocumentsExceptDeleted() {

        documentRepository.save(aDocument(1));
        documentRepository.save(aDocument(2));
        documentRepository.save(aDocument(3));
        documentRepository.save(aDocument(0));

        final List<Document> documents_1 = documentRepository.findByStatusNotInOrderByVendorReceivedDateAsc(Arrays.asList(Status.DELETED, Status.FILE_DELETED));
        assertThat(documents_1.size(), is(3));

        final Document document_resp_1 = documents_1.get(0);
        final Document document_resp_2 = documents_1.get(1);
        final Document document_resp_3 = documents_1.get(2);

        assertTrue(!document_resp_1.getVendorReceivedDate().isAfter(document_resp_2.getVendorReceivedDate()));
        assertTrue(!document_resp_2.getVendorReceivedDate().isAfter(document_resp_3.getVendorReceivedDate()));
    }

    @Test
    public void shouldFindAllDocuments() {

        final Document document_1 = documentRepository.save(aDocument(1));
        final Document document_2 = documentRepository.save(aDocument(2));
        final Document document_3 = documentRepository.save(aDocument(3));

        final List<Document> allDocuments = documentRepository.findAll();

        assertThat(allDocuments.size(), is(3));
        assertThat(allDocuments, containsInAnyOrder(document_1, document_2, document_3));
    }

    @Test
    public void shouldRemoveDocument() {

        final Document document = documentRepository.save(aDocument(1));
        assertThat(documentRepository.findBy(document.getScanDocumentId()), is(document));

        documentRepository.remove(document);

        assertThat(documentRepository.findBy(document.getScanDocumentId()), is(nullValue()));
    }

    @Test
    public void shouldGetDocumentCountGroupedBySourceStatusAndType() {

        final Document document_1 = documentRepository.save(aDocument(1));
        documentRepository.save(anotherDocumentLike(document_1));
        documentRepository.save(aDocument(2));

        final List<DocumentCountMapping> counts = documentRepository.getDocumentCount();

        assertThat(counts.size(), is(2));

        final DocumentCountMapping matchingGroup = counts.stream()
                .filter(count -> count.getSource() == document_1.getSource()
                        && count.getStatus() == document_1.getStatus()
                        && count.getType() == document_1.getType())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a grouped count for the duplicated document"));

        assertThat(matchingGroup.getCount(), is(2L));
    }

    @Test
    public void shouldFindDocumentsByCaseUrnOrCasePtiUrn() {

        final Document matchingByCaseUrn = documentRepository.save(aDocumentWithUrns("URN-1", "PTI-1"));
        final Document matchingByPtiUrn = documentRepository.save(aDocumentWithUrns("URN-2", "PTI-2"));
        documentRepository.save(aDocumentWithUrns("URN-3", "PTI-3"));

        final List<Document> result = documentRepository.findByCaseUrnInOrCasePTIUrnInOrderByCaseUrnAsc(Arrays.asList("URN-1", "PTI-2"));

        assertThat(result.size(), is(2));
        assertThat(result, containsInAnyOrder(matchingByCaseUrn, matchingByPtiUrn));
    }

    private Document aDocumentWithUrns(final String caseUrn, final String casePtiUrn) {
        return document()
                .withId(randomUUID())
                .withCaseId(randomUUID())
                .withSource(Source.CPS)
                .withStatus(Status.IN_PROGRESS)
                .withType(Type.values()[0])
                .withCaseUrn(caseUrn)
                .withCasePTIUrn(casePtiUrn)
                .withReceivedDateTime(new UtcClock().now().minusDays(1))
                .withVendorReceivedDate(new UtcClock().now().minusDays(1))
                .build();
    }

    private Document anotherDocumentLike(final Document template) {
        return document()
                .withId(randomUUID())
                .withCaseId(randomUUID())
                .withSource(template.getSource())
                .withStatus(template.getStatus())
                .withType(template.getType())
                .withReceivedDateTime(template.getReceivedDateTime())
                .withVendorReceivedDate(template.getVendorReceivedDate())
                .build();
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
