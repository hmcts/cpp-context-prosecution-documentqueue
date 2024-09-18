package uk.gov.moj.cpp.prosecution.documentqueue.event.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.CaseStatus;

import uk.gov.moj.cpp.prosecution.documentqueue.event.service.CaseStatusService;
import uk.gov.moj.cpp.prosecution.documentqueue.persistence.CaseStatusRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CaseStatusServiceTest {

    @Mock
    private CaseStatusRepository caseStatusRepository;

    @InjectMocks
    private CaseStatusService caseStatusService;

    @Test
    public void shouldSaveACaseStatus() throws Exception {

        final CaseStatus caseStatus = mock(CaseStatus.class);

        caseStatusService.saveCaseStatus(caseStatus);

        verify(caseStatusRepository).save(caseStatus);
    }

    @Test
    public void shouldFindCaseStatusById() throws Exception {

        final UUID caseStatusId = randomUUID();
        final CaseStatus caseStatus = mock(CaseStatus.class);
        final List<CaseStatus> result = new ArrayList<>();
        result.add(caseStatus);

        when(caseStatusRepository.findByCaseId(caseStatusId)).thenReturn(result);

        assertThat(caseStatusService.findByCaseId(caseStatusId).size(), is(1));
    }

    @Test
    public void shouldFindCaseStatusByIdAndStatus() throws Exception {

        final UUID caseStatusId = randomUUID();
        final CaseStatus caseStatus = mock(CaseStatus.class);
        final List<CaseStatus> result = new ArrayList<>();
        result.add(caseStatus);

        when(caseStatusRepository.findByCaseIdAndStatus(caseStatusId, Status.COMPLETED)).thenReturn(result);

        assertThat(caseStatusService.findByCaseIdAndStatus(caseStatusId, Status.COMPLETED).size(), is(1));
    }
}
