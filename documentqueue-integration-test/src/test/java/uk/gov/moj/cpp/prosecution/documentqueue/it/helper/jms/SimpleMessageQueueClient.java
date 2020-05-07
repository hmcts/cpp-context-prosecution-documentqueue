package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms;

import static java.util.Collections.emptyMap;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.SimpleFileClient.getFile;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.TopicChecker.waitUntilQueueIsEmpty;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.lang3.RandomUtils;

public class SimpleMessageQueueClient {

    private static long eventNumber = RandomUtils.nextLong(0L, Long.MAX_VALUE);

    public static void publishPrivateEvent(
            final EventName eventName,
            final String fileName) {
        publishPrivateEvent(eventName, fileName, emptyMap());
    }

    public static void publishPrivateEvent(
            final EventName eventName,
            final String fileName,
            final Map<String, String> valuesMap) {
        publishPrivateEvent(eventName, fileName, valuesMap, ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static void publicEventToTopic(
            final String eventName,
            final String topicName,
            final String fileName,
            final Map<String, String> valuesMap,
            final ZonedDateTime createdAt) {
        final JsonObject payload = getFile("json/" + fileName, valuesMap).asJsonObject();
        final Metadata metadata = newMetadata(eventName, createdAt);
        final JsonEnvelope envelope = envelopeFrom(metadata, payload);
        sendPrivateEvent(envelope, eventName,topicName);
        waitUntilQueueIsEmpty(topicName);
    }
    private static void sendPrivateEvent(final JsonEnvelope envelope, final String privateEventName,final String topicName) {
        try (final MessageProducerClient messageProducerClient = new MessageProducerClient()) {
            messageProducerClient.startProducer(topicName);
            messageProducerClient.sendMessage(privateEventName, envelope);
        }
    }
    public static void publishPrivateEvent(
            final EventName eventName,
            final String fileName,
            final Map<String, String> valuesMap,
            final ZonedDateTime createdAt) {
        final JsonObject payload = getFile("json/" + fileName, valuesMap).asJsonObject();
        final Metadata metadata = newMetadata(eventName.getEventName(), createdAt);
        final JsonEnvelope envelope = envelopeFrom(metadata, payload);
        sendPrivateEvent(envelope, eventName);
        waitUntilQueueIsEmpty(eventName.getTopicName());
    }

    private static void sendPrivateEvent(final JsonEnvelope envelope, final EventName eventName) {
        try (final MessageProducerClient messageProducerClient = new MessageProducerClient()) {
            messageProducerClient.startProducer(eventName.getTopicName());
            messageProducerClient.sendMessage(eventName.getEventName(), envelope);
        }
    }

    private static Metadata newMetadata(final String eventName, final ZonedDateTime createdAt) {
        eventNumber = eventNumber + 1;

        return metadataWithRandomUUID(eventName)
                .withStreamId(UUID.randomUUID())
                .withPosition(1L)
                .withEventNumber(eventNumber)
                .withPreviousEventNumber(eventNumber - 1)
                .withUserId("72a6daec-f4d0-4131-9af6-6726d2ef5ade")
                .createdAt(createdAt)
                .build();
    }
}
