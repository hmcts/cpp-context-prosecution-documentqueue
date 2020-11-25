package uk.gov.moj.cpp.prosecution.documentqueue.event.service;

import uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.moj.cpp.prosecution.documentqueue.persistence.CaseStatusRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class CaseStatusService {

    @Inject
    private CaseStatusRepository caseStatusRepository;

    public void saveCaseStatus(final CaseStatus caseStatus) {
        caseStatusRepository.save(caseStatus);
    }

    public List<CaseStatus> findByCaseId(final UUID caseId) {
        return caseStatusRepository.findByCaseId(caseId);
    }

    public List<CaseStatus> findByCaseIdAndStatus(final UUID caseId, final Status status) {
        return caseStatusRepository.findByCaseIdAndStatus(caseId, status);
    }
}
