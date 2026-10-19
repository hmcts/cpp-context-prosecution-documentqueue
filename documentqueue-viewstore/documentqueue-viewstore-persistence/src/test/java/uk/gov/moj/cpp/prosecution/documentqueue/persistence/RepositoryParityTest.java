package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus.CaseStatusBuilder.caseStatus;
import static uk.gov.moj.cpp.prosecution.documentqueue.entity.Document.DocumentBuilder.document;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * J17/J25 parity characterisation (J17 golden master) for the viewstore repositories.
 *
 * On J17 DocumentRepository and CaseStatusRepository are DeltaSpike repositories (derived finders and @Query) and
 * DocumentQueueRepository is a DeltaSpike repository using the framework's request-scoped EntityManager. The J25
 * upgrade rewrites all three by hand on a container-managed @PersistenceContext and Hibernate 6. Every assertion
 * below encodes the J17 result, for the finders production actually calls.
 *
 * Harness notes: hbm2ddl does not create the Liquibase column types, so the tests that read them change the column
 * first (document.*_ts are TIMESTAMP WITH TIME ZONE, case_status.status is VARCHAR(50)). The J17 harness runs H2 in
 * PostgreSQL mode and the J25 harness plain H2; they sort NULLs differently, so ordering tests use non-null keys.
 * The native housekeeping queries (getExpiredDocuments, getDocumentsEligibleForDeletionFromFileStore) use
 * PostgreSQL interval arithmetic that H2 cannot run; JavaUpgradeParityIT covers them against PostgreSQL.
 */
public class RepositoryParityTest {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    @RegisterExtension
    static final HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("documentqueue-test-persistence-unit");

    private final DocumentRepository documentRepository = new DocumentRepository();

    private final CaseStatusRepository caseStatusRepository = new CaseStatusRepository();

    private final DocumentQueueRepository documentQueueRepository = new DocumentQueueRepository();

    private EntityManager entityManager;

    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    public void clearTables() {
        hibernateTestEntityManagerProvider.injectEntityManagerInto(documentRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseStatusRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(documentQueueRepository);
        entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        originalDefaultTimeZone = TimeZone.getDefault();
        entityManager.createNativeQuery("delete from document").executeUpdate();
        entityManager.createNativeQuery("delete from case_status").executeUpdate();
    }

    @AfterEach
    public void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    // DQ-P01 (BC-01/02): findBy is the only single-result finder; an unknown id gives null on both repositories,
    // which the query view turns into an empty response.
    @Test
    public void dqP01_findByUnknownIdShouldReturnNull() {
        assertThat(documentRepository.findBy(randomUUID()), is(nullValue()));
        assertThat(caseStatusRepository.findBy(randomUUID()), is(nullValue()));
    }

    // DQ-P02: unlike contexts whose repositories go through the framework EntityManagerProducer, using these
    // repositories does not change the JVM default time zone on J17, so the server's own zone setting applies.
    @Test
    public void dqP02_repositoryUseShouldLeaveTheJvmDefaultTimeZoneUnchanged() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        documentRepository.save(aDocument("URN-P02", Source.CPS, Status.OUTSTANDING, Type.PLEA, ZonedDateTime.now(LONDON)));
        documentQueueRepository.getDocumentList(empty(), empty(), "vendorReceivedDate", "asc", 0, 1);

        assertThat(TimeZone.getDefault().getID(), is("Europe/London"));
    }

    // DQ-P03: DeltaSpike save() persists a new entity and returns the same, now managed, instance.
    @Test
    public void dqP03_saveOfNewEntityShouldReturnTheSameManagedInstance() {
        final Document document = aDocument("URN-P03", Source.CPS, Status.OUTSTANDING, Type.PLEA, ZonedDateTime.now(LONDON));
        final CaseStatus caseStatus = caseStatus().withId(randomUUID()).withCaseId(randomUUID()).withStatus(Status.COMPLETED).build();

        assertThat(documentRepository.save(document), sameInstance(document));
        assertThat(caseStatusRepository.save(caseStatus), sameInstance(caseStatus));
        assertThat(entityManager.contains(document), is(true));
        assertThat(entityManager.contains(caseStatus), is(true));
    }

    // DQ-P04 (BC-08): the query view hands Document timestamps (TIMESTAMP WITH TIME ZONE) to the response; pin the
    // zone the read-back ZonedDateTime carries on a UTC server.
    @Test
    public void dqP04_vendorReceivedDateShouldBeReadBackInTheJ17Zone() {
        entityManager.createNativeQuery("alter table document alter column vendor_received_date_ts set data type timestamp with time zone").executeUpdate();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        documentRepository.save(aDocument("URN-P04", Source.CPS, Status.OUTSTANDING, Type.PLEA, ZonedDateTime.of(2024, 6, 1, 10, 0, 0, 0, LONDON)));
        entityManager.flush();
        entityManager.clear();

        final Document reloaded = entityManager
                .createQuery("select d from Document d where d.caseUrn = :caseUrn", Document.class)
                .setParameter("caseUrn", "URN-P04")
                .getSingleResult();
        assertThat(reloaded.getVendorReceivedDate().toString(), is("2024-06-01T09:00Z[UTC]"));
    }

    // DQ-P05: CaseStatus.status has no @Enumerated, so it is mapped by ordinal into the VARCHAR(50) column, and
    // findByCaseIdAndStatus matches on that ordinal.
    @Test
    public void dqP05_caseStatusShouldBeStoredAndFoundByOrdinal() {
        entityManager.createNativeQuery("alter table case_status alter column status set data type varchar(50)").executeUpdate();
        final UUID caseId = randomUUID();
        final CaseStatus caseStatus = caseStatus().withId(randomUUID()).withCaseId(caseId).withStatus(Status.FILE_DELETED).build();

        caseStatusRepository.save(caseStatus);
        entityManager.flush();
        entityManager.clear();

        final Object stored = entityManager
                .createNativeQuery("select cast(status as varchar(50)) from case_status where case_id = ?1")
                .setParameter(1, caseId)
                .getSingleResult();
        assertThat(stored, is("3"));
        assertThat(caseStatusRepository.findByCaseIdAndStatus(caseId, Status.FILE_DELETED).size(), is(1));
        assertThat(caseStatusRepository.findByCaseIdAndStatus(caseId, Status.DELETED).size(), is(0));
        assertThat(caseStatusRepository.findByCaseId(caseId).get(0).getStatus(), is(Status.FILE_DELETED));
    }

    // DQ-P06: getDocumentCount feeds the documents-count query, which aggregates by status, type and source.
    @Test
    public void dqP06_documentCountShouldGroupBySourceStatusAndType() {
        final ZonedDateTime now = ZonedDateTime.now(LONDON);
        documentRepository.save(aDocument("URN-1", Source.CPS, Status.OUTSTANDING, Type.PLEA, now));
        documentRepository.save(aDocument("URN-2", Source.CPS, Status.OUTSTANDING, Type.PLEA, now));
        documentRepository.save(aDocument("URN-3", Source.BULKSCAN, Status.OUTSTANDING, Type.PLEA, now));
        documentRepository.save(aDocument("URN-4", Source.BULKSCAN, Status.COMPLETED, Type.APPLICATION, now));
        documentRepository.save(aDocument("URN-5", Source.OTHER, Status.DELETED, Type.OTHER, now));
        entityManager.flush();

        final List<String> counts = documentRepository.getDocumentCount().stream()
                .map(count -> count.getSource() + "/" + count.getStatus() + "/" + count.getType() + "=" + count.getCount())
                .collect(toList());

        assertThat(counts, containsInAnyOrder(
                "CPS/OUTSTANDING/PLEA=2",
                "BULKSCAN/OUTSTANDING/PLEA=1",
                "BULKSCAN/COMPLETED/APPLICATION=1",
                "OTHER/DELETED/OTHER=1"));
    }

    // DQ-P07: the document-ids-of-cases query groups consecutive rows by URN, so the order is part of the contract.
    @Test
    public void dqP07_documentsForCasesShouldBeOrderedByPtiUrnThenUrn() {
        final ZonedDateTime now = ZonedDateTime.now(LONDON);
        documentRepository.save(aDocument("B-URN", "PTI-2", "doc-1", now));
        documentRepository.save(aDocument("A-URN", "PTI-2", "doc-2", now));
        documentRepository.save(aDocument("C-URN", "PTI-1", "doc-3", now));
        documentRepository.save(aDocument("D-URN", "PTI-9", "doc-4", now));
        entityManager.flush();

        final List<String> names = documentRepository.findByCaseUrnInOrCasePTIUrnInOrderByCaseUrnAsc(asList("A-URN", "B-URN", "PTI-1")).stream()
                .map(Document::getDocumentName)
                .collect(toList());

        assertThat(names, contains("doc-3", "doc-2", "doc-1"));
    }

    // DQ-P08: the documents query pages and sorts in the database and returns the unpaged total. Without a status
    // filter, DELETED and FILE_DELETED documents are excluded.
    @Test
    public void dqP08_documentListShouldPageSortAndExcludeDeleted() {
        final ZonedDateTime start = ZonedDateTime.of(2024, 6, 1, 9, 0, 0, 0, LONDON);
        documentRepository.save(aDocument("URN-1", Source.CPS, Status.OUTSTANDING, Type.PLEA, start.plusMinutes(1)));
        documentRepository.save(aDocument("URN-2", Source.BULKSCAN, Status.IN_PROGRESS, Type.PLEA, start.plusMinutes(2)));
        documentRepository.save(aDocument("URN-3", Source.CPS, Status.DELETED, Type.PLEA, start.plusMinutes(3)));
        documentRepository.save(aDocument("URN-4", Source.CPS, Status.COMPLETED, Type.PLEA, start.plusMinutes(4)));
        documentRepository.save(aDocument("URN-5", Source.CPS, Status.FILE_DELETED, Type.PLEA, start.plusMinutes(5)));
        documentRepository.save(aDocument("URN-6", Source.CPS, Status.OUTSTANDING, Type.PLEA, start.plusMinutes(6)));
        entityManager.flush();

        final Pair<Integer, List<Document>> allDescending = documentQueueRepository.getDocumentList(empty(), empty(), "vendorReceivedDate", "desc", 1, 2);
        final Pair<Integer, List<Document>> cpsAscending = documentQueueRepository.getDocumentList(Optional.of(Source.CPS), empty(), "vendorReceivedDate", "asc", 0, 10);
        final Pair<Integer, List<Document>> deleted = documentQueueRepository.getDocumentList(empty(), Optional.of(Status.DELETED), "vendorReceivedDate", "asc", 0, 10);

        assertThat(allDescending.getLeft(), is(4));
        assertThat(caseUrns(allDescending.getRight()), contains("URN-4", "URN-2"));
        assertThat(cpsAscending.getLeft(), is(3));
        assertThat(caseUrns(cpsAscending.getRight()), contains("URN-1", "URN-4", "URN-6"));
        assertThat(caseUrns(deleted.getRight()), contains("URN-3"));
    }


    private static List<String> caseUrns(final List<Document> documents) {
        return documents.stream().map(Document::getCaseUrn).collect(toList());
    }


    private static Document aDocument(final String caseUrn, final Source source, final Status status, final Type type, final ZonedDateTime vendorReceivedDate) {
        return document()
                .withId(randomUUID())
                .withCaseId(randomUUID())
                .withCaseUrn(caseUrn)
                .withCasePTIUrn("PTI-" + caseUrn)
                .withDocumentName("document " + caseUrn)
                .withSource(source)
                .withStatus(status)
                .withType(type)
                .withVendorReceivedDate(vendorReceivedDate)
                .withReceivedDateTime(vendorReceivedDate)
                .build();
    }

    private static Document aDocument(final String caseUrn, final String casePtiUrn, final String documentName, final ZonedDateTime vendorReceivedDate) {
        return document().withDocument(aDocument(caseUrn, Source.CPS, Status.OUTSTANDING, Type.PLEA, vendorReceivedDate))
                .withCasePTIUrn(casePtiUrn)
                .withDocumentName(documentName)
                .build();
    }
}
