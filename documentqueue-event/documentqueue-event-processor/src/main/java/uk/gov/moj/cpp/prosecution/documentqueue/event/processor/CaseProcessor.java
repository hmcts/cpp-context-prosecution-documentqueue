package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.documentqueue.command.handler.RecordCaseStatus;
import uk.gov.moj.prosecution.documentqueue.domain.enums.CaseStatus;

import javax.inject.Inject;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProcessor.class);

    public static final String DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS = "documentqueue.command.record-case-status";

    @Inject
    private Sender sender;

    @Handles("public.progression.events.case-or-application-ejected")
    public void processCaseRejectedPublicEvent(final JsonEnvelope caseOrApplicationRejected) {
        final String prosecutionCaseId = caseOrApplicationRejected.payloadAsJsonObject().getString("prosecutionCaseId", "");
        if (prosecutionCaseId.isEmpty()) { // why we we need this?
            LOGGER.info("prosecutionCaseId so case not ejected");
        } else {
            // payload
            final RecordCaseStatus recordCaseStatus =
                    RecordCaseStatus
                            .recordCaseStatus()
                            .withStatus(CaseStatus.EJECTED)
                            .withCaseId(UUID.fromString(prosecutionCaseId))
                            .build();
            // envelope
            final Envelope<RecordCaseStatus> envelope =
                    envelop(recordCaseStatus)
                            .withName(DOCUMENTQUEUE_COMMAND_RECORD_CASE_STATUS)
                            .withMetadataFrom(caseOrApplicationRejected);
            sender.send(envelope);
        }

    }
}
