package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.mapping.DocumentCountMapping;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DocumentRepository {

    private static final String SOURCE = "source";

    private static final String STATUSES = "statuses";

    @PersistenceContext(unitName = "documentqueue")
    EntityManager entityManager;

    public Document save(final Document document) {
        return entityManager.merge(document);
    }

    public Document findBy(final UUID id) {
        return entityManager.find(Document.class, id);
    }

    public List<Document> findAll() {
        return entityManager.createQuery("SELECT d FROM Document d", Document.class).getResultList();
    }

    public void remove(final Document document) {
        entityManager.remove(entityManager.contains(document) ? document : entityManager.merge(document));
    }

    public List<Document> findBySourceAndStatusOrderByVendorReceivedDateAsc(final Source source, final Status status) {
        return entityManager.createQuery("SELECT d FROM Document d WHERE d.source = :source AND d.status = :status ORDER BY d.vendorReceivedDate ASC", Document.class)
                .setParameter(SOURCE, source).setParameter("status", status).getResultList();
    }

    public List<Document> findByStatusOrderByVendorReceivedDateAsc(final Status status) {
        return entityManager.createQuery("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.vendorReceivedDate ASC", Document.class)
                .setParameter("status", status).getResultList();
    }

    public List<DocumentCountMapping> getDocumentCount() {
        return entityManager.createQuery("SELECT new uk.gov.moj.cpp.prosecution.documentqueue.mapping.DocumentCountMapping(COUNT(d), d.source, d.status, d.type) FROM Document d GROUP BY d.source, d.status, d.type", DocumentCountMapping.class)
                .getResultList();
    }

    public List<Document> findByStatusNotInOrderByVendorReceivedDateAsc(final List<Status> statuses) {
        return entityManager.createQuery("SELECT d FROM Document d WHERE d.status NOT IN (:statuses) ORDER BY d.vendorReceivedDate ASC", Document.class)
                .setParameter(STATUSES, statuses).getResultList();
    }

    public List<Document> findBySourceAndStatusNotInOrderByVendorReceivedDateAsc(final Source source, final List<Status> statuses) {
        return entityManager.createQuery("SELECT d FROM Document d WHERE d.source = :source AND d.status NOT IN (:statuses) ORDER BY d.vendorReceivedDate ASC", Document.class)
                .setParameter(SOURCE, source).setParameter(STATUSES, statuses).getResultList();
    }

    public List<Document> findByTypeAndStatusNotInOrderByVendorReceivedDateAsc(final Type type, final List<Status> statuses) {
        return entityManager.createQuery("SELECT d FROM Document d WHERE d.type = :type AND d.status NOT IN (:statuses) ORDER BY d.vendorReceivedDate ASC", Document.class)
                .setParameter("type", type).setParameter(STATUSES, statuses).getResultList();
    }

    public List<Document> findBySourceAndTypeAndStatusNotInOrderByVendorReceivedDateAsc(final Source source, final Type type, final List<Status> statuses) {
        return entityManager.createQuery("SELECT d FROM Document d WHERE d.source = :source AND d.type = :type AND d.status NOT IN (:statuses) ORDER BY d.vendorReceivedDate ASC", Document.class)
                .setParameter(SOURCE, source).setParameter("type", type).setParameter(STATUSES, statuses).getResultList();
    }

    public List<Document> findByCaseUrnInOrCasePTIUrnInOrderByCaseUrnAsc(final List<String> urns) {
        return entityManager.createQuery("SELECT d FROM Document d WHERE d.caseUrn IN (:urns) OR d.casePTIUrn IN (:urns) ORDER BY d.casePTIUrn, d.caseUrn", Document.class)
                .setParameter("urns", urns).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Document> getExpiredDocuments(final int documentExpiryDays) {
        return entityManager.createNativeQuery(
                        "SELECT d.* FROM document d WHERE not exists (select cs.case_id from case_status cs where cs.case_id = d.case_id) and d.status in ('IN_PROGRESS', 'OUTSTANDING') and d.source = 'CPS' and d.received_date_time < now() - (interval '1' day) * :documentExpiryDays",
                        Document.class)
                .setParameter("documentExpiryDays", documentExpiryDays).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Document> getDocumentsEligibleForDeletionFromFileStore(final int days, final int maxResults) {
        return entityManager.createNativeQuery(
                        "SELECT d.* FROM document d WHERE not exists (select cs.case_id from case_status cs where cs.case_id = d.case_id) and d.status in ('COMPLETED', 'DELETED') and d.source = 'CPS' and d.received_date_time < now() - (interval '1' day) * :days order by d.received_date_time asc limit :maxResults",
                        Document.class)
                .setParameter("days", days).setParameter("maxResults", maxResults).getResultList();
    }
}
