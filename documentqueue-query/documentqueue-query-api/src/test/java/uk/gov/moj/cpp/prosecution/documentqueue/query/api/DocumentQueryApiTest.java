package uk.gov.moj.cpp.prosecution.documentqueue.query.api;


import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.DocumentContent;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.DocumentContentService;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.GetDocument;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DocumentQueryApiTest {

    @Mock
    private Requester requester;

    @Mock
    private DocumentContentService documentContentService;

    @InjectMocks
    private DocumentQueryApi documentQueryApi;

    @Test
    public void shouldHandleDocumentQuery() {
        assertThat(DocumentQueryApi.class, isHandlerClass(Component.QUERY_API)
                .with(method("queryDocuments")
                        .thatHandles("documentqueue.query.documents")
                        .withRequesterPassThrough()));
    }

    @Test
    public void shouldHandDocumentsCountsQuery() {
        assertThat(DocumentQueryApi.class, isHandlerClass(Component.QUERY_API)
                .with(method("queryDocumentCounts")
                        .thatHandles("documentqueue.query.documents-counts")
                        .withRequesterPassThrough()));
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

        when(requester.request(queryEnvelope, ScanDocument.class)).thenReturn(queryViewResponse);
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

        when(requester.request(queryEnvelope, ScanDocument.class)).thenReturn(queryViewResponse);
        when(queryViewResponse.payload()).thenReturn(null);

        final Envelope<GetDocument> getDocumentEnvelope = documentQueryApi.getDocument(queryEnvelope);

        assertThat(getDocumentEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope));
        assertThat(getDocumentEnvelope.payload(), is(nullValue()));

        verifyZeroInteractions(documentContentService);
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

        when(requester.request(queryEnvelope, DocumentContentView.class)).thenReturn(queryViewResponse);
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

        when(requester.request(queryEnvelope, DocumentContentView.class)).thenReturn(queryViewResponse);
        when(queryViewResponse.payload()).thenReturn(null);

        final Envelope<DocumentContent> documentContentEnvelope = documentQueryApi.getDocumentContent(queryEnvelope);

        assertThat(documentContentEnvelope.metadata(), withMetadataEnvelopedFrom(queryEnvelope));
        assertThat(documentContentEnvelope.payload(), is(nullValue()));

        verifyZeroInteractions(documentContentService);
    }
}
