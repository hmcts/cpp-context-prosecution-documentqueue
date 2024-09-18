package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus.CaseStatusBuilder.caseStatus;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus;

import java.util.List;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseStatusRepositoryTest extends BaseTransactionalJunit4Test  {

    @Inject
    private CaseStatusRepository caseStatusRepository;

    @Test
    public void shouldSaveAndFindDocumentById() throws Exception {

        final CaseStatus caseStatus_1 = aCaseStatus(Status.DELETED);
        final CaseStatus caseStatus_2 = aCaseStatus(Status.COMPLETED);
        final CaseStatus caseStatus_3 = aCaseStatus(Status.IN_PROGRESS);

        caseStatusRepository.save(caseStatus_1);
        caseStatusRepository.save(caseStatus_2);
        caseStatusRepository.save(caseStatus_3);

        assertThat(caseStatusRepository.findBy(caseStatus_1.getId()), is(caseStatus_1));
        assertThat(caseStatusRepository.findBy(caseStatus_2.getId()), is(caseStatus_2));
        assertThat(caseStatusRepository.findBy(caseStatus_3.getId()), is(caseStatus_3));
    }

    @Test
    public void shouldSaveAndFindCaseStatusByCaseId() throws Exception {

        final CaseStatus caseStatus_1 = aCaseStatus(Status.DELETED);
        final CaseStatus caseStatus_2 = aCaseStatus(Status.COMPLETED);
        final CaseStatus caseStatus_3 = aCaseStatus(Status.IN_PROGRESS);

        caseStatusRepository.save(caseStatus_1);
        caseStatusRepository.save(caseStatus_2);
        caseStatusRepository.save(caseStatus_3);

        final List<CaseStatus> listcaseStatuss_1 = caseStatusRepository.findByCaseId(caseStatus_2.getCaseId());
        assertThat(listcaseStatuss_1.size(), is(1));
        assertThat(listcaseStatuss_1.get(0), is(caseStatus_2));

        final List<CaseStatus> listcaseStatus_2 = caseStatusRepository.findByCaseId(caseStatus_3.getCaseId());
        assertThat(listcaseStatus_2.size(), is(1));

    }

    @Test
    public void shouldSaveAndFindCaseStatusByCaseIdAndStatus() throws Exception {

        final CaseStatus caseStatus_1 = aCaseStatus(Status.DELETED);
        final CaseStatus caseStatus_2 = aCaseStatus(Status.COMPLETED);

        caseStatusRepository.save(caseStatus_1);
        caseStatusRepository.save(caseStatus_2);

        final List<CaseStatus> listcaseStatuss_1 = caseStatusRepository.findByCaseIdAndStatus(caseStatus_1.getCaseId(),Status.DELETED);
        assertThat(listcaseStatuss_1.size(), is(1));
        assertThat(listcaseStatuss_1.get(0), is(caseStatus_1));

        final List<CaseStatus> listcaseStatus_2 = caseStatusRepository.findByCaseIdAndStatus(caseStatus_2.getCaseId(),Status.DELETED);
        assertThat(listcaseStatus_2.size(), is(0));

    }

    private CaseStatus aCaseStatus(final Status status) {
        return caseStatus()
                .withId(randomUUID())
                .withCaseId(randomUUID())
                .withStatus(status)
                .build();
    }
}
