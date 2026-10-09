package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus.CaseStatusBuilder.caseStatus;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CaseStatusRepositoryTest {

    private static final String PERSISTENCE_UNIT = "documentqueue-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private CaseStatusRepository caseStatusRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        caseStatusRepository = new CaseStatusRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseStatusRepository);
    }

    @Test
    public void shouldSaveAndFindDocumentById() {

        final CaseStatus caseStatus_1 = caseStatusRepository.save(aCaseStatus(Status.DELETED));
        final CaseStatus caseStatus_2 = caseStatusRepository.save(aCaseStatus(Status.COMPLETED));
        final CaseStatus caseStatus_3 = caseStatusRepository.save(aCaseStatus(Status.IN_PROGRESS));

        assertThat(caseStatusRepository.findBy(caseStatus_1.getId()), is(caseStatus_1));
        assertThat(caseStatusRepository.findBy(caseStatus_2.getId()), is(caseStatus_2));
        assertThat(caseStatusRepository.findBy(caseStatus_3.getId()), is(caseStatus_3));
    }

    @Test
    public void shouldSaveAndFindCaseStatusByCaseId() {

        caseStatusRepository.save(aCaseStatus(Status.DELETED));
        final CaseStatus caseStatus_2 = caseStatusRepository.save(aCaseStatus(Status.COMPLETED));
        final CaseStatus caseStatus_3 = caseStatusRepository.save(aCaseStatus(Status.IN_PROGRESS));

        final List<CaseStatus> listcaseStatuss_1 = caseStatusRepository.findByCaseId(caseStatus_2.getCaseId());
        assertThat(listcaseStatuss_1.size(), is(1));
        assertThat(listcaseStatuss_1.get(0), is(caseStatus_2));

        final List<CaseStatus> listcaseStatus_2 = caseStatusRepository.findByCaseId(caseStatus_3.getCaseId());
        assertThat(listcaseStatus_2.size(), is(1));

    }

    @Test
    public void shouldSaveAndFindCaseStatusByCaseIdAndStatus() {

        final CaseStatus caseStatus_1 = caseStatusRepository.save(aCaseStatus(Status.DELETED));
        final CaseStatus caseStatus_2 = caseStatusRepository.save(aCaseStatus(Status.COMPLETED));

        final List<CaseStatus> listcaseStatuss_1 = caseStatusRepository.findByCaseIdAndStatus(caseStatus_1.getCaseId(), Status.DELETED);
        assertThat(listcaseStatuss_1.size(), is(1));
        assertThat(listcaseStatuss_1.get(0), is(caseStatus_1));

        final List<CaseStatus> listcaseStatus_2 = caseStatusRepository.findByCaseIdAndStatus(caseStatus_2.getCaseId(), Status.DELETED);
        assertThat(listcaseStatus_2.size(), is(0));

    }

    @Test
    public void shouldFindAllCaseStatuses() {

        final CaseStatus caseStatus_1 = caseStatusRepository.save(aCaseStatus(Status.DELETED));
        final CaseStatus caseStatus_2 = caseStatusRepository.save(aCaseStatus(Status.COMPLETED));
        final CaseStatus caseStatus_3 = caseStatusRepository.save(aCaseStatus(Status.IN_PROGRESS));

        final List<CaseStatus> allCaseStatuses = caseStatusRepository.findAll();

        assertThat(allCaseStatuses.size(), is(3));
        assertThat(allCaseStatuses, containsInAnyOrder(caseStatus_1, caseStatus_2, caseStatus_3));
    }

    @Test
    public void shouldRemoveCaseStatus() {

        final CaseStatus caseStatus = caseStatusRepository.save(aCaseStatus(Status.COMPLETED));
        assertThat(caseStatusRepository.findBy(caseStatus.getId()), is(caseStatus));

        caseStatusRepository.remove(caseStatus);

        assertThat(caseStatusRepository.findBy(caseStatus.getId()), is(nullValue()));
    }

    private CaseStatus aCaseStatus(final Status status) {
        return caseStatus()
                .withId(randomUUID())
                .withCaseId(randomUUID())
                .withStatus(status)
                .build();
    }
}
