package uk.gov.moj.cpp.prosecution.documentqueue.query.api;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import uk.gov.justice.prosecution.documentqueue.domain.model.Completed;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentsCount;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.prosecution.documentqueue.domain.model.Total;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.DocumentContent;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.DocumentContentService;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.GetDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.DocumentQueryView;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentQueryApiTest {

    @Mock
    private DocumentQueryView documentQueryView;
    @Mock
    private DocumentContentService documentContentService;

    @InjectMocks
    private DocumentQueryApi documentQueryApi;

    @Test
    public void shouldHandleDocumentQuery() {
        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.documents"),
                createObjectBuilder());
        final Envelope<JsonObject> queryViewResponse = mock(Envelope.class);
        reset(documentQueryView);
        when(documentQueryView.getDocuments(queryEnvelope)).thenReturn(queryViewResponse);

        assertThat(documentQueryApi.queryDocuments(queryEnvelope), is(queryViewResponse));
    }

    @Test
    public void shouldHandDocumentsCountsQuery() {
        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.documents-counts"),
                createObjectBuilder());
        final Envelope<DocumentsCount> queryViewResponse = mock(Envelope.class);
        reset(documentQueryView);
        when(documentQueryView.getDocumentsCount(queryEnvelope)).thenReturn(queryViewResponse);

        assertThat(documentQueryApi.queryDocumentCounts(queryEnvelope), is(queryViewResponse));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandGetDocument() {
        final UUID documentId = randomUUID();
        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.get-document"),
                createObjectBuilder().add("documentId", documentId.toString()));

        final Envelope<ScanDocument> queryViewResponse = mock(Envelope.class);
        final ScanDocument documentView = mock(ScanDocument.class);
        final GetDocument getDocument = mock(GetDocument.class);

        reset(documentQueryView);
        when(documentQueryView.getDocument(queryEnvelope)).thenReturn(queryViewResponse);
        when(queryViewResponse.payload()).thenReturn(documentView);
        when(documentContentService.getDocument(documentView)).thenReturn(getDocument);

        final Envelope<GetDocument> getDocumentEnvelope = documentQueryApi.getDocument(queryEnvelope);

        assertThat(getDocumentEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope));
        assertThat(getDocumentEnvelope.payload(), is(getDocument));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnNullGetDocumentForDocumentIdIfTheDocumentIsNotReturnedByTheQueryView() {

        final UUID documentId = randomUUID();
        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.get-document"),
                createObjectBuilder().add("documentId", documentId.toString()));

        final Envelope<ScanDocument> queryViewResponse = mock(Envelope.class);

        reset(documentQueryView);
        when(documentQueryView.getDocument(queryEnvelope)).thenReturn(queryViewResponse);
        when(queryViewResponse.payload()).thenReturn(null);

        final Envelope<GetDocument> getDocumentEnvelope = documentQueryApi.getDocument(queryEnvelope);

        assertThat(getDocumentEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope));
        assertThat(getDocumentEnvelope.payload(), is(nullValue()));

        verifyNoInteractions(documentContentService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnDocumentContentForDocumentId() {

        final UUID documentId = randomUUID();
        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.document-content"),
                createObjectBuilder().add("documentId", documentId.toString()));

        final Envelope<DocumentContentView> queryViewResponse = mock(Envelope.class);
        final DocumentContentView documentContentView = mock(DocumentContentView.class);
        final DocumentContent documentContent = mock(DocumentContent.class);
        reset(documentQueryView);
        when(documentQueryView.getDocumentContent(queryEnvelope)).thenReturn(queryViewResponse);
        when(queryViewResponse.payload()).thenReturn(documentContentView);
        when(documentContentService.getDocumentContent(documentContentView)).thenReturn(documentContent);

        final Envelope<DocumentContent> documentContentEnvelope = documentQueryApi.getDocumentContent(queryEnvelope);

        assertThat(documentContentEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope));
        assertThat(documentContentEnvelope.payload(), is(documentContent));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnNullDocumentContentForDocumentIdIfTheDocumentIsNotReturnedByTheQueryView() {

        final UUID documentId = randomUUID();
        final JsonEnvelope queryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("documentqueue.query.document-content"),
                createObjectBuilder().add("documentId", documentId.toString()));

        final Envelope<DocumentContentView> queryViewResponse = mock(Envelope.class);

        reset(documentQueryView);
        when(documentQueryView.getDocumentContent(queryEnvelope)).thenReturn(queryViewResponse);
        when(queryViewResponse.payload()).thenReturn(null);

        final Envelope<DocumentContent> documentContentEnvelope = documentQueryApi.getDocumentContent(queryEnvelope);

        assertThat(documentContentEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope));
        assertThat(documentContentEnvelope.payload(), is(nullValue()));

        verifyNoInteractions(documentContentService);
    }
}