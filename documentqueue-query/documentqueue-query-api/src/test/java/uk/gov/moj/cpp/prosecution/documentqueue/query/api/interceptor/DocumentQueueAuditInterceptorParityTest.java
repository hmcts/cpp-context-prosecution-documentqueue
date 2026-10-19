package uk.gov.moj.cpp.prosecution.documentqueue.query.api.interceptor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.audit.AuditService;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.interceptors.DocumentQueueAuditInterceptor;

import java.io.StringReader;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * J17/J25 parity characterisation (J17 golden master), DQ-M03 / BC-11.
 *
 * The audit interceptor removes "content" from the document-content response before auditing it. The J25 upgrade
 * swaps Json.createObjectBuilder() (Glassfish javax.json) for the framework JsonObjects builder (Parsson), so pin the
 * exact audited payload for values a builder is most likely to treat differently: JSON null, decimals, large
 * integers, booleans, escaped and non-ASCII strings, nesting and key order.
 */
@ExtendWith(MockitoExtension.class)
class DocumentQueueAuditInterceptorParityTest {

    private static final String RESPONSE = "{"
            + "\"documentId\":\"2b0f6f6e-3c7a-4f3e-9d0e-6d2b8a1c4e51\","
            + "\"content\":\"JVBERi0xLjQK\","
            + "\"fileName\":\"say \\\"hi\\\" / café.pdf\","
            + "\"materialId\":null,"
            + "\"size\":12.50,"
            + "\"bigNumber\":12345678901234567890,"
            + "\"availableAsPdf\":true,"
            + "\"tags\":[null,1,\"two\",{\"content\":\"nested content is kept\"}]"
            + "}";

    @Mock
    private AuditService auditService;

    @Mock
    private InterceptorContext interceptorContext;

    @Mock
    private InterceptorChain interceptorChain;

    @Captor
    private ArgumentCaptor<JsonEnvelope> auditedEnvelope;

    @InjectMocks
    private DocumentQueueAuditInterceptor documentQueueAuditInterceptor;

    @Test
    void dqM03_shouldAuditTheJ17DocumentContentResponseWithoutContent() {
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("documentqueue.query.document-content").build(),
                parse(RESPONSE));
        when(interceptorContext.getComponentName()).thenReturn("QUERY_API");
        when(interceptorChain.processNext(interceptorContext)).thenReturn(interceptorContext);
        when(interceptorContext.outputEnvelope()).thenReturn(Optional.of(response));

        documentQueueAuditInterceptor.process(interceptorContext, interceptorChain);

        verify(auditService, times(2)).audit(auditedEnvelope.capture(), eq("QUERY_API"));
        assertThat(auditedEnvelope.getAllValues().get(1).payloadAsJsonObject().toString(), is("{"
                + "\"documentId\":\"2b0f6f6e-3c7a-4f3e-9d0e-6d2b8a1c4e51\","
                + "\"fileName\":\"say \\\"hi\\\" / café.pdf\","
                + "\"materialId\":null,"
                + "\"size\":12.50,"
                + "\"bigNumber\":12345678901234567890,"
                + "\"availableAsPdf\":true,"
                + "\"tags\":[null,1,\"two\",{\"content\":\"nested content is kept\"}]"
                + "}"));
    }

    private static JsonObject parse(final String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
