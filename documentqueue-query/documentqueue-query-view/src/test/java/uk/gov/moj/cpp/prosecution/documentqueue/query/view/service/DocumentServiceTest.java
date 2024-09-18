package uk.gov.moj.cpp.prosecution.documentqueue.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Source.BULKSCAN;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Source.CPS;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.COMPLETED;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.IN_PROGRESS;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import org.apache.commons.lang3.tuple.Pair;
import uk.gov.justice.cpp.prosecution.documentqueue.domain.DocumentIdsOfCases;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentsCount;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.mapping.DocumentCountMapping;
import uk.gov.moj.cpp.prosecution.documentqueue.persistence.DocumentQueueRepository;
import uk.gov.moj.cpp.prosecution.documentqueue.persistence.DocumentRepository;
import uk.gov.moj.cpp.prosecution.documentqueue.query.view.converter.DocumentConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock
    private DocumentConverter documentConverter;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentQueueRepository documentQueueRepository;

    @Mock
    private Logger logger;

    @InjectMocks
    private DocumentService documentService;

    @Test
    public void shouldQueryByStatusAndReturnScanDocumentsPayload() {

        final JsonEnvelope envelope = createEnvelope("documentqueue.query.documents",
                createObjectBuilder().add("status", OUTSTANDING.toString()).build());
        final UUID documentId = randomUUID();

        final List<Document> documents = Stream.of(new Document.DocumentBuilder().withId(documentId).build()).collect(toList());
        final List<ScanDocument> scanDocuments = singletonList(ScanDocument.scanDocument().withDocumentId(documentId).build());

        when(documentQueueRepository.getDocumentList(Optional.empty(),Optional.of(OUTSTANDING),"statusUpdatedDate", "desc", 0, 50)).thenReturn(Pair.of(2,documents));
        when(documentConverter.convertToScanDocuments(documents)).thenReturn(scanDocuments);

        final List<ScanDocument> resultScanDocuments = documentService.getDocuments(Optional.empty(),Optional.of(OUTSTANDING),"statusUpdatedDate", "desc", 0, 50).getRight();

        verify(documentQueueRepository).getDocumentList(Optional.empty(),Optional.of(OUTSTANDING),"statusUpdatedDate", "desc", 0, 50);
        verify(documentConverter).convertToScanDocuments(documents);

        assertThat(resultScanDocuments, is(resultScanDocuments));
    }

    @Test
    public void shouldQueryBySourceAndReturnScanDocumentsPayload() {

        final JsonEnvelope envelope = createEnvelope("documentqueue.query.documents",
                createObjectBuilder().add("source", BULKSCAN.toString()).build());
        final UUID documentId = randomUUID();

        final List<Document> documents = Stream.of(new Document.DocumentBuilder().withId(randomUUID()).build()).collect(toList());
        final List<ScanDocument> scanDocuments = singletonList(ScanDocument.scanDocument().withDocumentId(documentId).build());

        when(documentQueueRepository.getDocumentList(Optional.of(BULKSCAN),Optional.empty(),"statusUpdatedDate", "desc", 0, 50)).thenReturn(Pair.of(2,documents));


        when(documentConverter.convertToScanDocuments(documents)).thenReturn(scanDocuments);

        final List<ScanDocument> resultScanDocuments = documentService.getDocuments(Optional.of(BULKSCAN),Optional.empty(),"statusUpdatedDate", "desc", 0, 50).getRight();

        verify(documentQueueRepository).getDocumentList(Optional.of(BULKSCAN),Optional.empty(),"statusUpdatedDate", "desc", 0, 50);
        verify(documentConverter).convertToScanDocuments(documents);

        assertThat(resultScanDocuments, is(resultScanDocuments));
    }

    @Test
    public void shouldQueryBySourceAndStatusAndReturnScanDocumentsPayload() {

        final JsonEnvelope envelope = createEnvelope("documentqueue.query.documents",
                createObjectBuilder().add("source", BULKSCAN.toString())
                        .add("status", OUTSTANDING.toString())
                        .build());
        final UUID documentId = randomUUID();

        final List<Document> documents = Stream.of(new Document.DocumentBuilder().withId(randomUUID()).build()).collect(toList());
        final List<ScanDocument> scanDocuments = singletonList(ScanDocument.scanDocument().withDocumentId(documentId).build());

        when(documentQueueRepository.getDocumentList(Optional.of(BULKSCAN),Optional.of(OUTSTANDING),"statusUpdatedDate", "desc", 0, 50)).thenReturn(Pair.of(2,documents));
        when(documentConverter.convertToScanDocuments(documents)).thenReturn(scanDocuments);

        final List<ScanDocument> resultScanDocuments = documentService.getDocuments(Optional.of(BULKSCAN),Optional.of(OUTSTANDING),"statusUpdatedDate", "desc", 0, 50).getRight();

        verify(documentQueueRepository).getDocumentList(Optional.of(BULKSCAN),Optional.of(OUTSTANDING),"statusUpdatedDate", "desc", 0, 50);

        assertThat(resultScanDocuments, is(resultScanDocuments));
    }

    @Test
    public void shouldQueryAllAndReturnScanDocumentsPayload() {

        final JsonEnvelope envelope = createEnvelope("documentqueue.query.documents",
                createObjectBuilder().build());
        final UUID documentId = randomUUID();

        final List<Document> documents = Stream.of(new Document.DocumentBuilder().withId(randomUUID()).build()).collect(toList());
        final List<ScanDocument> scanDocuments = singletonList(ScanDocument.scanDocument().withDocumentId(documentId).build());


        when(documentQueueRepository.getDocumentList(Optional.empty(),Optional.empty(),"vendorReceivedDate", "asc", 0, 50)).thenReturn(Pair.of(2,documents));
        when(documentConverter.convertToScanDocuments(documents)).thenReturn(scanDocuments);

        final List<ScanDocument> resultScanDocuments = documentService.getDocuments(Optional.empty(),Optional.empty(),"vendorReceivedDate", "asc", 0, 50).getRight();

        verify(documentQueueRepository).getDocumentList(Optional.empty(),Optional.empty(),"vendorReceivedDate", "asc", 0, 50);
        verify(documentConverter).convertToScanDocuments(documents);

        assertThat(resultScanDocuments, is(scanDocuments));
    }

    @Test
    public void testGetDocumentsCount() {
        final List<DocumentCountMapping> documentCounts = new ArrayList<>();
        documentCounts.add(new DocumentCountMapping(2L, BULKSCAN, OUTSTANDING, Type.PLEA));
        documentCounts.add(new DocumentCountMapping(4L, BULKSCAN, IN_PROGRESS, Type.APPLICATION));
        documentCounts.add(new DocumentCountMapping(2L, BULKSCAN, COMPLETED, Type.CORRESPONDENCE));
        documentCounts.add(new DocumentCountMapping(6L, BULKSCAN, OUTSTANDING, Type.OTHER));
        documentCounts.add(new DocumentCountMapping(2L, CPS, OUTSTANDING, Type.PLEA));

        when(documentRepository.getDocumentCount()).thenReturn(documentCounts);

        final DocumentsCount count = documentService.getDocumentsCount();
        assertThat(count, notNullValue());
        assertThat(count.getCompleted(), notNullValue());
        assertThat(count.getOutstanding(), notNullValue());
        assertThat(count.getInProgress(), notNullValue());

        assertThat(count.getInProgress().getApplications().getVolume(), is(4));
        assertThat(count.getInProgress().getApplications().getBulkScan(), is(4));
        assertThat(count.getInProgress().getTotalCount(), is(4));

        assertThat(count.getOutstanding().getTotalCount(), is(10));
        assertThat(count.getOutstanding().getPleas().getBulkScan(), is(2));
        assertThat(count.getOutstanding().getPleas().getVolume(), is(4));
        assertThat(count.getOutstanding().getOther().getVolume(), is(6));
        assertThat(count.getOutstanding().getOther().getBulkScan(), is(6));

        assertThat(count.getCompleted().getCorrespondence().getVolume(), is(2));
        assertThat(count.getCompleted().getCorrespondence().getBulkScan(), is(2));
    }

    @Test
    public void shouldReturnDocumentContentViewForDocumentId() {

        final UUID documentId = randomUUID();
        final Document document = new Document.DocumentBuilder().withId(documentId).build();
        final DocumentContentView documentContentView = mock(DocumentContentView.class);

        when(documentRepository.findBy(documentId)).thenReturn(document);
        when(documentConverter.convertToDocumentContentView(document)).thenReturn(documentContentView);

        final Optional<DocumentContentView> documentServiceDocument = documentService.getDocument(documentId);

        assertThat(documentServiceDocument.isPresent(), is(true));
        assertThat(documentServiceDocument.get(), is(documentContentView));
    }

    @Test
    public void shouldReturnOptionalEmptyIfDocumentNotFound() {

        final UUID documentId = fromString("25bb9886-c97c-4aa6-a254-75211b50536c");

        when(documentRepository.findBy(documentId)).thenReturn(null);

        final Optional<DocumentContentView> documentServiceDocument = documentService.getDocument(documentId);

        assertThat(documentServiceDocument.isPresent(), is(false));

        verify(logger).info("No document found in document table with id '25bb9886-c97c-4aa6-a254-75211b50536c'");
    }

    @Test
    public void shouldReturnDocumentForDocumentId() {
        final UUID documentId = randomUUID();
        final Document document = new Document.DocumentBuilder().withId(documentId).build();
        final ScanDocument scanDocument = mock(ScanDocument.class);

        when(documentRepository.findBy(documentId)).thenReturn(document);
        when(documentConverter.convertToScanDocument(document)).thenReturn(scanDocument);

        final Optional<ScanDocument> documentServiceDocument = documentService.getDocumentById(documentId);

        assertThat(documentServiceDocument.isPresent(), is(true));
        assertThat(documentServiceDocument.get(), is(scanDocument));
    }

    @Test
    public void shouldReturnOptionalEmptyDocumentIfDocumentNotFound() {
        final UUID documentId = fromString("25bb9886-c97c-4aa6-a254-75211b50536c");

        when(documentRepository.findBy(documentId)).thenReturn(null);

        final Optional<ScanDocument> documentServiceDocument = documentService.getDocumentById(documentId);

        assertThat(documentServiceDocument.isPresent(), is(false));

        verify(logger).info("No document found in document table with id '25bb9886-c97c-4aa6-a254-75211b50536c'");
    }

    @Test
    public void shouldGetDocumentIdsForCasesWithCaseUrns() {
        final boolean isPriUrn = false;
        final DocumentIdsOfCases documentIdsOfCases = getDocumentIdsOfCases(isPriUrn);

        assertThat(documentIdsOfCases.getProsecutionCases().get(0).getCaseUrn(), is("urn1"));
        assertThat(documentIdsOfCases.getProsecutionCases().get(1).getCaseUrn(), is("urn2"));
    }

    @Test
    public void shouldGetDocumentIdsForCasesWithCasePtiUrns() {
        final boolean isPriUrn = true;
        final DocumentIdsOfCases documentIdsOfCases = getDocumentIdsOfCases(isPriUrn);

        assertThat(documentIdsOfCases.getProsecutionCases().get(0).getCasePTIUrn(), is("urn1"));
        assertThat(documentIdsOfCases.getProsecutionCases().get(1).getCasePTIUrn(), is("urn2"));
    }

    private DocumentIdsOfCases getDocumentIdsOfCases(final boolean isPriUrn) {
        final List<String> caseUrns = asList("urn1", "urn2");
        final Document document1 = getDocument("urn1", isPriUrn);
        final Document document2 = getDocument("urn2", isPriUrn);

        final List<Document> documents  =  new ArrayList<>();
        documents.add(document1);
        documents.add(document2);
        when(documentRepository.findByCaseUrnInOrCasePTIUrnInOrderByCaseUrnAsc(caseUrns)).thenReturn(documents);
        final DocumentIdsOfCases documentIdsOfCases = documentService.getDocumentIdsForCases(caseUrns).get();

        assertThat(documentIdsOfCases.getProsecutionCases().size(), is(2));
        assertThat(documentIdsOfCases.getProsecutionCases().get(0).getDocumentIds().get(0), is(document1.getScanDocumentId()));
        assertThat(documentIdsOfCases.getProsecutionCases().get(1).getDocumentIds().get(0), is(document2.getScanDocumentId()));
        return documentIdsOfCases;
    }

    private Document getDocument(final String urn, final boolean isPtiUrn) {
        final Document.DocumentBuilder documentBuilder = new Document.DocumentBuilder()
                .withId(UUID.randomUUID())
                .withCaseUrn("urn1");

        if (isPtiUrn) {
            documentBuilder.withCasePTIUrn(urn);
        } else {
            documentBuilder.withCaseUrn(urn);
        }
        return documentBuilder.build();
    }

}
