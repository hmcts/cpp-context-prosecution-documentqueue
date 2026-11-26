package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms;

import org.apache.commons.lang3.RandomUtils;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPrivateJmsMessageProducerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.SimpleFileClient.getFile;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventNames.CONTEXT_NAME;

public class JmsMessageSender {
    private final JmsMessageProducerClient publicMessageProducerClient;
    private final JmsMessageProducerClient privateMessageProducerClient;
    private static long eventNumber = RandomUtils.nextLong(0L, Long.MAX_VALUE); //TODO find out a different way to deal with this counter

    public JmsMessageSender() {
        publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        privateMessageProducerClient = newPrivateJmsMessageProducerClientProvider(CONTEXT_NAME).getMessageProducerClient();
    }

    public void sendPublicEvent(
            final String eventName,
            final String fileName,
            final Map<String, String> valuesMap,
            final ZonedDateTime createdAt) {
        final JsonObject payload = getFile("json/" + fileName, valuesMap).asJsonObject();
        final Metadata metadata = newMetadata(eventName, createdAt);
        final JsonEnvelope envelope = envelopeFrom(metadata, payload);

        publicMessageProducerClient.sendMessage(eventName, envelope);
    }

    public void sendPrivateEvent(
            final EventName eventName,
            final String fileName,
            final Map<String, String> valuesMap){
        final ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);
        final JsonObject payload = getFile("json/" + fileName, valuesMap).asJsonObject();
        final Metadata metadata = newMetadata(eventName.getEventName(), createdAt);
        final JsonEnvelope envelope = envelopeFrom(metadata, payload);

        privateMessageProducerClient.sendMessage(eventName.getEventName(), envelope);
        //TopicChecker.waitUntilQueueIsEmpty("jms.topic.documentqueue.event"); //TODO get this into sendMessage functionality itself
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
