package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedEjected;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedFiltered;
import uk.gov.moj.cpp.documentqueue.event.DocumentLinkedToCase;
import uk.gov.moj.cpp.documentqueue.event.DocumentNotLinkedToCase;
import uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class CPPCaseTest {

    private CPPCase cppCase = new CPPCase();

    @Test
    public void shouldLinkDocumentToCase() {
        final UUID caseId = UUID.randomUUID();
        final UUID documentId = UUID.randomUUID();
        final Document document = new Document.Builder()
                .withCaseId(caseId)
                .withScanDocumentId(documentId)
                .build();
        final List<Object> eventList = cppCase.linkDocumentToCase(document).collect(Collectors.toList());

        assertThat(eventList.size(), is(1));

        final Object event = eventList.get(0);
        assertThat(event, instanceOf(DocumentLinkedToCase.class));
        assertThat(((DocumentLinkedToCase) event).getDocument().getCaseId(), is(caseId));
        assertThat(((DocumentLinkedToCase) event).getDocument().getScanDocumentId(), is(documentId));
    }

    @Test
    public void shouldNotLinkDocumentToCase() {
        final UUID caseId = UUID.randomUUID();
        cppCase.updateCaseStatus(CaseStatus.EJECTED,caseId);

        final UUID documentId = UUID.randomUUID();
        final Document document = new Document.Builder()
                .withCaseId(caseId)
                .withScanDocumentId(documentId)
                .build();
        final List<Object> eventList = cppCase.linkDocumentToCase(document).collect(Collectors.toList());
        assertThat(eventList.size(), is(1));

        final Object event = eventList.get(0);
        assertThat(event, instanceOf(DocumentNotLinkedToCase.class));
        assertThat(((DocumentNotLinkedToCase) event).getDocument().getCaseId(), is(caseId));
        assertThat(((DocumentNotLinkedToCase) event).getDocument().getScanDocumentId(), is(documentId));
    }

    @Test
    public void shouldMarkAsEjectedWhenCaseStatusIsRejected() {
        final UUID caseId = UUID.randomUUID();
        final UUID documentId = UUID.randomUUID();
        final Document document = new Document.Builder()
                .withCaseId(caseId)
                .withScanDocumentId(documentId)
                .build();
        cppCase.linkDocumentToCase(document);

        final List<Object> eventList = cppCase
                .updateCaseStatus(CaseStatus.EJECTED, caseId)
                .collect(Collectors.toList());
        assertThat(eventList.size(), is(1));

        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseMarkedEjected.class));
        assertThat(((CaseMarkedEjected) event).getCaseId(), is(caseId));
    }

    @Test
    public void shouldMarkAsFilteredWhenCaseStatusIsFiltered() {
        final UUID caseId = UUID.randomUUID();
        final UUID documentId = UUID.randomUUID();
        final Document document = new Document.Builder()
                .withCaseId(caseId)
                .withScanDocumentId(documentId)
                .build();
        cppCase.linkDocumentToCase(document);

        final List<Object> eventList = cppCase
                .updateCaseStatus(CaseStatus.FILTERED, caseId)
                .collect(Collectors.toList());
        assertThat(eventList.size(), is(1));

        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseMarkedFiltered.class));
        assertThat(((CaseMarkedFiltered) event).getCaseId(), is(caseId));
    }

}