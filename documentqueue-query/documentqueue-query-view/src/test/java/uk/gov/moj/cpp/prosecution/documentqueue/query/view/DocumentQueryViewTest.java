package uk.gov.moj.cpp.prosecution.documentqueue.query.view;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import uk.gov.justice.prosecution.documentqueue.domain.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentsCount;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.service.DocumentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.activemq.artemis.utils.Env;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DocumentQueryViewTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private ListToJsonArrayConverter<ScanDocument> listToJsonArrayConverter;

    @InjectMocks
    private DocumentQueryView documentQueryView;

    @Test
    public void shouldGetDocumentsResponse() {

        final JsonEnvelope envelope = createEnvelope("documentqueue.query.documents",
                createObjectBuilder().build());
        final UUID documentId = randomUUID();
        final List<ScanDocument> scanDocuments = singletonList(ScanDocument.scanDocument().withDocumentId(documentId).build());

        final JsonObject jsonObject = createObjectBuilder().add("id", documentId.toString()).build();
        final JsonArray jsonArray = Json.createArrayBuilder()
                .add(jsonObject)
                .build();
        final JsonValue expectedResult = createObjectBuilder().add("documents", jsonArray).build();

        when(documentService.getDocuments(envelope)).thenReturn(scanDocuments);
        when(listToJsonArrayConverter.convert(scanDocuments)).thenReturn(jsonArray);

        final Envelope<JsonObject> documents = documentQueryView.getDocuments(envelope);

        assertThat(documents.metadata(), withMetadataEnvelopedFrom(envelope).withName("documentqueue.query.documents"));
        assertThat(documents.payload(), is(expectedResult));
    }

    @Test
    public void shouldReturnDocumentCountResponse() {

        final JsonEnvelope queryEnvelope = createEnvelope("documentqueue.query.documents-counts",
                createObjectBuilder().build());
        final DocumentsCount expectedDocumentsCount = mock(DocumentsCount.class);

        when(documentService.getDocumentsCount()).thenReturn(expectedDocumentsCount);

        final Envelope<DocumentsCount> documentsCount = documentQueryView.getDocumentsCount(queryEnvelope);

        assertThat(documentsCount.metadata(), withMetadataEnvelopedFrom(queryEnvelope).withName("documentqueue.query.documents-counts"));
        assertThat(documentsCount.payload(), is(expectedDocumentsCount));
    }

    @Test
    public void shouldReturnDocumentContentViewPayloadIfFound() {

        final UUID documentId = randomUUID();

        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.document-content"),
                createObjectBuilder().add("documentId", documentId.toString()));

        final DocumentContentView documentContentView = DocumentContentView.documentContentView().withDocumentId(documentId).build();

        when(documentService.getDocument(documentId)).thenReturn(Optional.of(documentContentView));

        final Envelope<DocumentContentView> documentContentViewEnvelope = documentQueryView.getDocumentContent(queryEnvelope);

        assertThat(documentContentViewEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope).withName("documentqueue.query.document-content"));
        assertThat(documentContentViewEnvelope.payload(), is(documentContentView));
    }

    @Test
    public void shouldReturnNullPayloadIfNotFound() {

        final UUID documentId = randomUUID();

        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.document-content"),
                createObjectBuilder().add("documentId", documentId.toString()));

        final DocumentContentView documentContentView = DocumentContentView.documentContentView().withDocumentId(documentId).build();

        when(documentService.getDocument(documentId)).thenReturn(Optional.empty());

        final Envelope<DocumentContentView> documentContentViewEnvelope = documentQueryView.getDocumentContent(queryEnvelope);

        assertThat(documentContentViewEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope).withName("documentqueue.query.document-content"));
        assertThat(documentContentViewEnvelope.payload(), is(nullValue()));
    }

    @Test
    public void shouldReturnSingleDocument() {
        final UUID documentId = UUID.randomUUID();
        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.get-document"),
                createObjectBuilder().add("documentId", documentId.toString()));
        final ScanDocument expectedScanDocument = mock(ScanDocument.class);


        when(documentService.getDocumentById(UUID.fromString(queryEnvelope.payloadAsJsonObject().getString("documentId")))).thenReturn(expectedScanDocument);

        final Envelope<ScanDocument> scanDocumentEnvelope = documentQueryView.getDocument(queryEnvelope);

        assertThat(scanDocumentEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope).withName("documentqueue.query.get-document"));
        assertThat(scanDocumentEnvelope.payload(), is(expectedScanDocument));


    }
}