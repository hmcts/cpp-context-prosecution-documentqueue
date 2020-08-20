package uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate;


import uk.gov.moj.cpp.platform.test.serializable.AggregateSerializableChecker;

import org.junit.Test;

public class QueueDocumentTest {

    private AggregateSerializableChecker aggregateSerializableChecker = new AggregateSerializableChecker();

    @Test
    public void shouldCheckAggregatesAreSerializable() {
        aggregateSerializableChecker.checkAggregatesIn("uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate");
    }
}
