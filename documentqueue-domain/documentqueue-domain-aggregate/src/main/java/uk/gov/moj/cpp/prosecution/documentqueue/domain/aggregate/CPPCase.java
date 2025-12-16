package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.documentqueue.event.CaseMarkedEjected.caseMarkedEjected;
import static uk.gov.moj.cpp.documentqueue.event.CaseMarkedSubmissionSucceeded.caseMarkedSubmissionSucceeded;
import static uk.gov.moj.cpp.documentqueue.event.CaseMarkedFiltered.caseMarkedFiltered;
import static uk.gov.moj.cpp.documentqueue.event.DocumentLinkedToCase.documentLinkedToCase;
import static uk.gov.moj.cpp.documentqueue.event.DocumentNotLinkedToCase.documentNotLinkedToCase;
import static uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus.EJECTED;
import static uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus.FILTERED;
import static uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus.COMPLETED;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.prosecution.documentqueue.domain.Document;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedEjected;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedFiltered;
import uk.gov.moj.cpp.documentqueue.event.CaseMarkedSubmissionSucceeded;
import uk.gov.moj.cpp.documentqueue.event.DocumentLinkedToCase;
import uk.gov.moj.cpp.documentqueue.event.DocumentNotLinkedToCase;
import uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class CPPCase implements Aggregate {

    private String ptiUrn;

    private String caseUrn;

    private UUID caseId;

    private CaseStatus caseStatus;

    private List<UUID> documents = new ArrayList<>();

    public Stream<Object> linkDocumentToCase(final Document document) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (null == caseStatus) {
            streamBuilder.add(
                    documentLinkedToCase()
                            .withDocument(document)
                            .build());
        } else {
            streamBuilder.add(
                    documentNotLinkedToCase()
                            .withDocument(document)
                            .withReason("The document is not linked to the case as the status of the case is " + caseStatus.toString())
                            .build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> updateCaseStatus(final CaseStatus status, final UUID caseId) {
        if (EJECTED.equals(status)) {
            return markCaseAsRejected(caseId);
        } else if (FILTERED.equals(status)) {
            return markCaseAsFiltered(caseId);
        } else if (COMPLETED.equals(status)) {
            return markCaseAsSubmissionSucceeded(caseId);
        }
        return null;
    }

    private Stream<Object> markCaseAsRejected(final UUID caseId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(caseMarkedEjected()
                .withCaseId(caseId)
                .build());
        return apply(streamBuilder.build());
    }

    private Stream<Object> markCaseAsFiltered(final UUID caseId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(caseMarkedFiltered()
                .withCaseId(caseId)
                .build());
        return apply(streamBuilder.build());
    }

    private Stream<Object> markCaseAsSubmissionSucceeded(final UUID caseId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(caseMarkedSubmissionSucceeded()
                .withCaseId(caseId)
                .build());
        return apply(streamBuilder.build());
    }
    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(DocumentLinkedToCase.class).apply(e -> {
                    if (null == this.ptiUrn) {
                        this.ptiUrn = e.getDocument().getCasePTIUrn();
                    }
                    if (null == this.caseUrn) {
                        this.caseUrn = e.getDocument().getCaseUrn();
                    }
                    if (null == caseId) {
                        this.caseId = e.getDocument().getCaseId();
                    }
                    this.documents.add(e.getDocument().getScanDocumentId());
                }),
                when(DocumentNotLinkedToCase.class).apply(e ->
                        doNothing()
                ),
                when(CaseMarkedEjected.class).apply(e ->
                        caseStatus = EJECTED
                ),
                when(CaseMarkedFiltered.class).apply(e ->
                        caseStatus = FILTERED
                ),                when(CaseMarkedSubmissionSucceeded.class).apply(e ->
                        doNothing()
                ));
    }

    public List<UUID> getDocuments() {
        return Collections.unmodifiableList(documents);
    }
    public CaseStatus getCaseStatus() {
        return this.caseStatus;
    }

}
