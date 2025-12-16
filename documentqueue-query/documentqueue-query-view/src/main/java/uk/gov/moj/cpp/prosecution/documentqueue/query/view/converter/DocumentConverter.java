package uk.gov.moj.cpp.prosecution.documentqueue.query.view.converter;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.model.DocumentContentView;
import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.util.List;
import java.util.stream.Collectors;

public class DocumentConverter {

    public List<ScanDocument> convertToScanDocuments(final List<Document> documents) {
        return documents.stream()
                .map(this::convertToScanDocument)
                .collect(Collectors.toList());
    }

    public DocumentContentView convertToDocumentContentView(final Document document) {
        return new DocumentContentView.Builder()
                .withDocumentId(document.getScanDocumentId())
                .withSource(document.getSource())
                .withStatus(document.getStatus())
                .withType(document.getType())
                .withFileName(document.getFileName())
                .withMaterialId(document.getMaterialId())
                .withFileServiceId(document.getFileServiceId())
                .build();
    }

    public ScanDocument convertToScanDocument(final Document document) {
        final ScanDocument.Builder documentBuilder = new ScanDocument.Builder();

        return documentBuilder
                .withCaseUrn(document.getCaseUrn())
                .withCasePTIUrn(document.getCasePTIUrn())
                .withDocumentId(document.getScanDocumentId())
                .withEnvelopeId(document.getEnvelopeId())
                .withDocumentType(document.getType())
                .withFileName(document.getFileName())
                .withProsecutorAuthorityCode(document.getProsecutorAuthorityCode())
                .withReceivedDate(document.getVendorReceivedDate())
                .withSource(document.getSource())
                .withStatus(document.getStatus())
                .withStatusUpdatedDate(document.getStatusUpdatedDate())
                .withDocumentName(document.getDocumentName())
                .withAvailableAsPdf(availableAsPdf(document))
                .withStatusCode(document.getStatusCode())
                .build();
    }

    private boolean availableAsPdf(final Document document) {
        return Source.BULKSCAN.equals(document.getSource()) || (null != document.getFileName() && document.getFileName().trim().toLowerCase().endsWith(".pdf"));
    }
}
