package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.AttachDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.DocumentQueryView;

import java.io.StringReader;
import java.util.UUID;

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
import org.slf4j.Logger;

/**
 * J17/J25 parity characterisation (J17 golden master), DQ-M03 / BC-11.
 *
 * attachDocument copies the court document JSON through enrichMaterialId, which the J25 upgrade moves from
 * Json.createObjectBuilder() (Glassfish javax.json) to the framework JsonObjects builder (Parsson). Pin the exact
 * progression.add-court-document payload for a court document carrying JSON null, decimals, large integers,
 * booleans, escaped and non-ASCII strings, nesting and key order.
 */
@ExtendWith(MockitoExtension.class)
class AttachDocumentParityTest {

    private static final UUID DOCUMENT_ID = UUID.fromString("2b0f6f6e-3c7a-4f3e-9d0e-6d2b8a1c4e51");
    private static final UUID MATERIAL_ID = UUID.fromString("7c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f");
    private static final UUID FILE_SERVICE_ID = UUID.fromString("9e8d7c6b-5a49-4382-9170-6f5e4d3c2b1a");

    private static final String COURT_DOCUMENT = "{"
            + "\"courtDocumentId\":\"0f1e2d3c-4b5a-4968-8776-655443322110\","
            + "\"name\":\"say \\\"hi\\\" / café.pdf\","
            + "\"amendmentDate\":null,"
            + "\"materials\":[{\"id\":\"replaced\",\"receivedDateTime\":\"2024-06-01T09:00:00.000Z\",\"dropped\":1}],"
            + "\"documentCategory\":{\"defendantDocument\":{\"prosecutionCaseId\":\"case-1\",\"defendants\":[null,\"d-1\"]}},"
            + "\"size\":12.50,"
            + "\"bigNumber\":12345678901234567890,"
            + "\"containsFinancialMeans\":false"
            + "}";

    @Mock
    private Sender sender;

    @Mock
    private Logger logger;

    @Mock
    private FileService fileService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private DocumentQueryView documentQueryView;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> sentEnvelopes;

    @InjectMocks
    private DocumentqueueEventProcessor documentqueueEventProcessor;

    @Test
    void dqM03_shouldSendTheJ17AddCourtDocumentPayload() {
        when(objectToJsonObjectConverter.convert(any())).thenReturn(parse(COURT_DOCUMENT));
        when(documentQueryView.getDocumentContent(any())).thenReturn(envelopeFrom(
                metadataWithRandomUUID("documentqueue.query.document-content"),
                DocumentContentView.documentContentView().withDocumentId(DOCUMENT_ID).withMaterialId(MATERIAL_ID).withFileServiceId(FILE_SERVICE_ID).build()));

        documentqueueEventProcessor.attachDocument(envelopeFrom(
                metadataWithRandomUUID("documentqueue.event.attach-document-requested"),
                AttachDocument.attachDocument().withDocumentId(DOCUMENT_ID).build()));

        verify(sender, atLeastOnce()).send(sentEnvelopes.capture());
        final JsonObject addCourtDocument = sentEnvelopes.getAllValues().stream()
                .filter(envelope -> "progression.add-court-document".equals(envelope.metadata().name()))
                .findFirst()
                .orElseThrow(AssertionError::new)
                .payload();
        assertThat(addCourtDocument.toString(), is("{"
                + "\"materialId\":\"7c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f\","
                + "\"courtDocument\":{"
                + "\"courtDocumentId\":\"0f1e2d3c-4b5a-4968-8776-655443322110\","
                + "\"name\":\"say \\\"hi\\\" / café.pdf\","
                + "\"amendmentDate\":null,"
                + "\"materials\":[{\"id\":\"7c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f\",\"receivedDateTime\":\"2024-06-01T09:00:00.000Z\"}],"
                + "\"documentCategory\":{\"defendantDocument\":{\"prosecutionCaseId\":\"case-1\",\"defendants\":[null,\"d-1\"]}},"
                + "\"size\":12.50,"
                + "\"bigNumber\":12345678901234567890,"
                + "\"containsFinancialMeans\":false"
                + "}}"));
    }

    private static JsonObject parse(final String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
