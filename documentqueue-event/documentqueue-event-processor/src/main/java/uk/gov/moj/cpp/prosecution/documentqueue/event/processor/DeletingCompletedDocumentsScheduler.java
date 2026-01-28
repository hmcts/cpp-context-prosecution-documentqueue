package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.lang.Integer.parseInt;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.COMPLETED;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.DocumentQueryView;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class DeletingCompletedDocumentsScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletingCompletedDocumentsScheduler.class);

    private static final String TIMER_TIMEOUT_INFO = "DocumentQueueEventProcessorScheduler timer triggered.";
    private static final String DOCUMENT_QUEUE_QUERY_GET_DOCUMENTS_BY_STATUS = "documentqueue.query.documents";
    private static final String UPDATE_COMPLETED_DOCUMENTS = "documentqueue.command.update-document-status";

    @Inject
    @Value(key = "deleteAfterCompletedDays", defaultValue = "30")
    private String deleteAfterCompletedDays;

    @Inject
    @Value(key = "deleteAfterCompletedDaysForBulkScan", defaultValue = "90")
    private String deleteAfterCompletedDaysForBulkScan;

    @Inject
    @Value(key = "documentQueueEventProcessorSchedulerIntervalMillis", defaultValue = "100000")
    private String documentQueueEventProcessorSchedulerIntervalMillis;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private DocumentQueryView documentQueryView;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Resource
    private TimerService timerService;

    @PostConstruct
    public void init() {

        timerService.createIntervalTimer(
            30000L,
            Long.parseLong(this.getDocumentQueueEventProcessorSchedulerIntervalMillis()),
            new TimerConfig(TIMER_TIMEOUT_INFO, false));
    }

    @Timeout
    public void startTimer() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DocumentQueue scheduler triggers.");
        }

        LOGGER.info("DeletingCompletedDocumentsScheduler triggers.");

        final List<ScanDocument> completedDocuments = getAllCompletedDocuments();

        final List<ScanDocument> documentsTobeDeleted = completedDocuments.stream()
                .filter(document -> DAYS.between(document.getStatusUpdatedDate(), ZonedDateTime.now()) > getDocumentToBeDeletedInDays(document.getSource()))
                .collect(Collectors.toList());

        LOGGER.info("number of documents to be deleted are {}", documentsTobeDeleted.size());
        documentsTobeDeleted.forEach(document ->
                sender.send(envelopeFrom(metadataBuilder().withId(randomUUID()).withName(UPDATE_COMPLETED_DOCUMENTS).build(), buildPayload(document.getDocumentId()))));

    }

    private JsonObject buildPayload(final UUID documentId) {
        return createObjectBuilder()
                .add("documentId", documentId.toString())
                .add("status", "DELETED")
                .build();
    }

    private int getDocumentToBeDeletedInDays(final Source source) {
        if (Source.BULKSCAN.equals(source)) {
            return parseInt(this.getDeleteAfterCompletedDaysForBulkScan());
        }
        return parseInt(this.getDeleteAfterCompletedDays());
    }

    @PreDestroy
    public void cleanup() {
        timerService.getTimers().forEach(Timer::cancel);
    }

    private List<ScanDocument> getAllCompletedDocuments() {

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(DOCUMENT_QUEUE_QUERY_GET_DOCUMENTS_BY_STATUS).build(),
                createObjectBuilder().add("status", COMPLETED.toString()).build());

        final Envelope<JsonObject> actionedDocuments = documentQueryView.getDocuments(requestEnvelope);
        return convertToList(actionedDocuments.payload().getJsonArray("documents"), ScanDocument.class);
    }

    private <T> List<T> convertToList(final JsonArray jsonArray, final Class<T> clazz) {
        final List<T> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(this.jsonObjectToObjectConverter.convert(jsonArray.getJsonObject(i), clazz));
        }
        return list;
    }

    public String getDeleteAfterCompletedDays() {
        return deleteAfterCompletedDays;
    }

    public String getDocumentQueueEventProcessorSchedulerIntervalMillis() {
        return documentQueueEventProcessorSchedulerIntervalMillis;
    }

    public String getDeleteAfterCompletedDaysForBulkScan() {
        return deleteAfterCompletedDaysForBulkScan;
    }
}