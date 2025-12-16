package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;

import static java.util.Arrays.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;

import uk.gov.justice.cpp.prosecution.documentqueue.domain.ProsecutionCase;
import uk.gov.justice.cpp.prosecution.documentqueue.domain.ProsecutionCaseDocStatus;
import uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesActioned;
import uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesRequested;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class DeleteDocumentsTest {

    private DeleteDocuments deleteDocuments = new DeleteDocuments();

    @Test
    public void shouldSaveRequestReceived() {
        final List<Object> eventList = deleteDocuments
                .saveRequestReceived(null, asList("urn1")).collect(Collectors.toList());

        assertThat(eventList.size(), is(1));

        final Object event = eventList.get(0);
        assertThat(event, instanceOf(DeleteDocumentsOfCasesRequested.class));
        assertThat(((DeleteDocumentsOfCasesRequested) event).getCasePTIUrns(), contains("urn1"));
    }

    @Test
    public void shouldDeleteDocumentsOfCases() {
        final UUID caseId = UUID.randomUUID();
        final ProsecutionCaseDocStatus prosecutionCaseDocStatus = ProsecutionCaseDocStatus
                .prosecutionCaseDocStatus()
                .withCaseId(caseId)
                .withCasePTIUrn("urn1")
                .build();

        final List<Object> eventList = deleteDocuments
                .deleteDocumentsOfCases(asList(prosecutionCaseDocStatus))
                .collect(Collectors.toList());

        assertThat(eventList.size(), is(1));

        final Object event = eventList.get(0);
        assertThat(event, instanceOf(DeleteDocumentsOfCasesActioned.class));
        assertThat(((DeleteDocumentsOfCasesActioned) event)
                .getProsecutionCases().get(0).getCaseId(), is(caseId));
        assertThat(((DeleteDocumentsOfCasesActioned) event)
                .getProsecutionCases().get(0).getCasePTIUrn(), is("urn1"));
    }


}