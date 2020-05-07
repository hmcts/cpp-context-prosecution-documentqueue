package uk.gov.moj.cpp.prosecution.documentqueue.command.handler;

import uk.gov.moj.cpp.prosecution.documentqueue.domain.aggregate.QueueDocument;

import java.util.stream.Stream;

public interface DocumentStatusProcessor {
    Stream<Object> apply(QueueDocument queueDocument);
}