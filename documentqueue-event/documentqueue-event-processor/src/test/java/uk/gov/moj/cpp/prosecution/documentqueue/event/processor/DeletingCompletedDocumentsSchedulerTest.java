package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.DocumentQueryView;

import java.time.ZonedDateTime;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeletingCompletedDocumentsSchedulerTest {

    @InjectMocks
    private DeletingCompletedDocumentsScheduler deletingCompletedDocumentsScheduler;

    @Mock
    private DocumentQueryView documentQueryView;

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @BeforeEach
    public void init() throws IllegalAccessException {
        writeField(deletingCompletedDocumentsScheduler, "deleteAfterCompletedDays", "30", true);
        writeField(deletingCompletedDocumentsScheduler, "deleteAfterCompletedDaysForBulkScan", "90", true);
    }

    @Test
    public void testCompletedDocumentsDeleting() {

        final ScanDocument scanDocument = ScanDocument.scanDocument()
                .withDocumentId(randomUUID())
                .withStatus(Status.COMPLETED)
                .withSource(Source.BULKSCAN)
                .withStatusUpdatedDate(ZonedDateTime.now().minusDays(91l)).build();

        final JsonObject completedDocument = createObjectBuilder()
                .add("documentId", scanDocument.getDocumentId().toString())
                .add("status", scanDocument.getStatus().toString())
                .add("source", scanDocument.getSource().toString())
                .add("statusUpdatedDate", scanDocument.getStatusUpdatedDate().toString()).build();

        Envelope<JsonObject> completedDocuments = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("documentqueue.query.documents").build(),
                createObjectBuilder().add("documents",
                        createArrayBuilder().add(completedDocument)
                                .build())
                        .build());

        when(documentQueryView.getDocuments(any())).thenReturn(completedDocuments);
        when(jsonObjectToObjectConverter.convert(completedDocument, ScanDocument.class)).thenReturn(scanDocument);

        deletingCompletedDocumentsScheduler.startTimer();

        verify(documentQueryView, times(1)).getDocuments(any());
        verify(sender).send(envelopeArgumentCaptor.capture());
        verifyNoMoreInteractions(sender);

        final Envelope<JsonObject> jsonObjectEnvelope = envelopeArgumentCaptor.getValue();

        assertThat(jsonObjectEnvelope.payload().getString("documentId"), is(scanDocument.getDocumentId().toString()));
        assertThat(jsonObjectEnvelope.payload().getString("status"), is("DELETED"));
    }

}
