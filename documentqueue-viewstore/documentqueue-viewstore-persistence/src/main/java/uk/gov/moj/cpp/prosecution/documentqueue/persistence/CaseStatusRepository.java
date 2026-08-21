package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseStatusRepository {

    @PersistenceContext(unitName = "documentqueue")
    EntityManager entityManager;

    public CaseStatus save(final CaseStatus caseStatus) {
        return entityManager.merge(caseStatus);
    }

    public CaseStatus findBy(final UUID id) {
        return entityManager.find(CaseStatus.class, id);
    }

    public List<CaseStatus> findAll() {
        return entityManager.createQuery("SELECT cs FROM CaseStatus cs", CaseStatus.class).getResultList();
    }

    public void remove(final CaseStatus caseStatus) {
        entityManager.remove(entityManager.contains(caseStatus) ? caseStatus : entityManager.merge(caseStatus));
    }

    public List<CaseStatus> findByCaseId(final UUID caseId) {
        return entityManager.createQuery("SELECT cs FROM CaseStatus cs WHERE cs.caseId = :caseId", CaseStatus.class)
                .setParameter("caseId", caseId).getResultList();
    }

    public List<CaseStatus> findByCaseIdAndStatus(final UUID caseId, final Status status) {
        return entityManager.createQuery("SELECT cs FROM CaseStatus cs WHERE cs.caseId = :caseId AND cs.status = :status", CaseStatus.class)
                .setParameter("caseId", caseId).setParameter("status", status).getResultList();
    }
}
