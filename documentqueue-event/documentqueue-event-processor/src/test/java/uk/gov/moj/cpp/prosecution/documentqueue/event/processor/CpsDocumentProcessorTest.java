package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.LinkDocumentToCase;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.event.converter.LinkDocumentToCaseEnveloper;
import uk.gov.moj.cpp.prosecution.documentqueue.service.material.MaterialService;
import uk.gov.moj.cpp.prosecution.documentqueue.service.material.pojo.MaterialMetadata;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentReviewRequired;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsDocumentProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private LinkDocumentToCaseEnveloper outstandingDocumentEnveloper;

    @Mock
    private FileService fileService;

    @Mock
    MaterialService materialService;

    @InjectMocks
    private CpsDocumentProcessor cpsDocumentProcessor;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnFileName() throws Exception {
        final Envelope<DocumentReviewRequired> inputEnvelope = mock(Envelope.class);
        final DocumentReviewRequired documentReviewRequired = mock(DocumentReviewRequired.class);
        final UUID fileStoreId = UUID.randomUUID();
        final String fileName_1 = "XVBN22.pdf";
        final JsonObject fileMetadata = mock(JsonObject.class);
        final Envelope<LinkDocumentToCase> linkDocumentToCaseEnvelope = mock(Envelope.class);

        when(inputEnvelope.payload()).thenReturn(documentReviewRequired);
        when(documentReviewRequired.getFileStoreId()).thenReturn(fileStoreId);
        when(fileService.retrieveMetadata(fileStoreId)).thenReturn(Optional.of(fileMetadata));
        when(fileMetadata.getString("fileName")).thenReturn(fileName_1);
        when(outstandingDocumentEnveloper.toEnvelope(inputEnvelope, fileName_1)).thenReturn(linkDocumentToCaseEnvelope);

        cpsDocumentProcessor.processDocumentReviewRequiredEnvelope(inputEnvelope);
        verify(sender).send(linkDocumentToCaseEnvelope);
    }

    @Test
    public void shouldSendOutstandingDocument() {
        final UUID materialId = UUID.randomUUID();
        final String fileName = "MockFile_1";
        final ReceiveOutstandingDocument receiveOutstandingDocument = mock(ReceiveOutstandingDocument.class);
        final Envelope<uk.gov.moj.cps.progression.domain.event.DocumentReviewRequired> documentReviewRequiredEnvelope = mock(Envelope.class);
        final uk.gov.moj.cps.progression.domain.event.DocumentReviewRequired documentReviewRequired = mock(uk.gov.moj.cps.progression.domain.event.DocumentReviewRequired.class);
        final Envelope<LinkDocumentToCase> outstandingDocumentEnvelope = mock(Envelope.class);
        final MaterialMetadata metadata = mock(MaterialMetadata.class);

        when(documentReviewRequiredEnvelope.payload()).thenReturn(documentReviewRequired);
        when(documentReviewRequired.getMaterialId()).thenReturn(materialId);
        when(materialService.materialMetaDataForMaterialId(materialId)).thenReturn(metadata);
        when(metadata.getFileName()).thenReturn(fileName);
        when(outstandingDocumentEnveloper.toReviewEnvelope(documentReviewRequiredEnvelope, fileName)).thenReturn(outstandingDocumentEnvelope);
        cpsDocumentProcessor.processDocumentReviewRequired(documentReviewRequiredEnvelope);

        verify(sender).send(outstandingDocumentEnvelope);
    }
}