package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseStatusRepository extends EntityRepository<CaseStatus, UUID> {
    List<CaseStatus> findByCaseId(final UUID caseId);
    List<CaseStatus> findByCaseIdAndStatus(final UUID caseId, final Status status);
}
