package uk.gov.moj.cpp.prosecution.documentqueue.query.api.interceptors;

import static java.util.Objects.nonNull;

import uk.gov.justice.services.core.audit.AuditService;
import uk.gov.justice.services.core.interceptor.Interceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentQueueAuditInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentQueueAuditInterceptor.class);

    private final List eventNames = Collections.unmodifiableList(Arrays.asList("documentqueue.query.document-content"));

    @Inject
    AuditService auditService;

    public InterceptorContext process(final InterceptorContext interceptorContext, final InterceptorChain interceptorChain) {
        final String component = interceptorContext.getComponentName();
        this.recordAudit(interceptorContext.inputEnvelope(), component);
        final InterceptorContext outputContext = interceptorChain.processNext(interceptorContext);
        final Optional<JsonEnvelope> jsonEnvelope = outputContext.outputEnvelope();
        if (nonNull(jsonEnvelope)) {
            jsonEnvelope.ifPresent(envelope -> {
                if (eventNames.contains(envelope.metadata().name())) {
                    if(LOGGER.isInfoEnabled()) {
                        LOGGER.info("removing content from documentqueue.query.document-content");
                    }
                    this.recordAudit(removeContentFromPayload(envelope), component);
                } else {
                    this.recordAudit(envelope, component);
                }
            });
        }
        return outputContext;
    }

    private JsonObject removeProperty(final JsonObject origin, final String key) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            if (entry.getKey().equals(key)) {
                continue;
            } else {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    private JsonEnvelope removeContentFromPayload(final JsonEnvelope envelope) {
        JsonObject payload = null;
        if (!envelope.payloadIsNull()) {
            payload = envelope.payloadAsJsonObject();
        }

        if (nonNull(payload) && payload.containsKey("content")) {
            final JsonObject jsonObject = removeProperty(payload, "content");
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("after content removed for documentqueue.query.document-content  {}", jsonObject);
            }
            return JsonEnvelope.envelopeFrom(envelope.metadata(), jsonObject);
        }
        return envelope;
    }

    private void recordAudit(final JsonEnvelope jsonEnvelope, final String component) {
        this.auditService.audit(jsonEnvelope, component);
    }
}
