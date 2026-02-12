package uk.gov.moj.cpp.prosecution.documentqueue.query.api.interceptor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.audit.AuditService;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.interceptors.DocumentQueueAuditInterceptor;

import java.util.Optional;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentQueueAuditInterceptorTest  {

    public static final String FILE_CONTENT = "test.pdf";
    public static final String FILE_NAME = "fileName";
    @Mock
  private AuditService auditService;

  @Mock
  private InterceptorContext interceptorContext;

  @Mock
  private InterceptorChain interceptorChain;

  @InjectMocks
  private DocumentQueueAuditInterceptor documentQueueAuditInterceptor;

  @Test
  public void shouldProcessTest(){
      final JsonObject jsonObject = JsonObjects.createObjectBuilder()
              .add("content", "test binary")
              .add(FILE_NAME, FILE_CONTENT).build();
      final Optional<JsonEnvelope>jsonEnvelope = Optional.of(JsonEnvelope.envelopeFrom(
              JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("documentqueue.query.document-content").build(),
              jsonObject));
      when(interceptorContext.getComponentName()).thenReturn("test");
      when(interceptorChain.processNext(interceptorContext)).thenReturn(interceptorContext);
      when(interceptorContext.outputEnvelope()).thenReturn(jsonEnvelope);
      final InterceptorContext interceptorContextUpdated = documentQueueAuditInterceptor.process(interceptorContext, interceptorChain);
      final JsonObject payload = interceptorContextUpdated.outputEnvelope().get().payloadAsJsonObject();
      assertThat(payload.getString(FILE_NAME), is(FILE_CONTENT));

  }

}
