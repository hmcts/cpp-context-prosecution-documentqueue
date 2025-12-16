package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesRequested.deleteDocumentsOfCasesRequested;

import uk.gov.justice.cpp.prosecution.documentqueue.domain.ProsecutionCaseDocStatus;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesActioned;
import uk.gov.moj.cpp.documentqueue.event.DeleteDocumentsOfCasesRequested;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class DeleteDocuments implements Aggregate {

    public static final UUID DELETE_DOCUMENTS_STEAM_ID = UUID.fromString("48249fd3-e8a6-48ac-a909-0468e3bf1939");

    public Stream<Object> saveRequestReceived(final List<String> caseUrns,
                                              final List<String> casePTIUrns)  {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(deleteDocumentsOfCasesRequested()
                .withCaseUrns(caseUrns)
                .withCasePTIUrns(casePTIUrns)
                .build());
        return apply(streamBuilder.build());
    }

    public Stream<Object> deleteDocumentsOfCases(final List<ProsecutionCaseDocStatus> prosecutionCaseList) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(DeleteDocumentsOfCasesActioned.deleteDocumentsOfCasesActioned()
                .withProsecutionCases(prosecutionCaseList)
                .build());
        return apply(streamBuilder.build());
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DeleteDocumentsOfCasesRequested.class).apply(e ->
                        doNothing()
                ),
                when(DeleteDocumentsOfCasesActioned.class).apply(e ->
                        doNothing()
                ));
    }
}
