package uk.gov.moj.cpp.prosecution.documentqueue.event.converter;

import static uk.gov.justice.prosecution.documentqueue.domain.Document.document;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument.receiveOutstandingDocument;

import uk.gov.justice.json.schemas.stagingbulkscan.ScanEnvelopeDocument;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.moj.cpp.prosecution.documentqueue.command.handler.ReceiveOutstandingDocument;

import java.util.UUID;


public class ScanDocumentConverter {

    public ReceiveOutstandingDocument asOutstandingDocument(final ScanDocument scanDocument, final UUID scanEnvelopeId) {

        return receiveOutstandingDocument().withOutstandingDocument(
                document()
                        .withEnvelopeId(scanEnvelopeId.toString())
                        .withActionedBy(scanDocument.getActionedBy())
                        .withCasePTIUrn(scanDocument.getCasePTIUrn())
                        .withCaseUrn(scanDocument.getCaseUrn())
                        .withDocumentControlNumber(scanDocument.getDocumentControlNumber())
                        .withDocumentName(scanDocument.getDocumentName())
                        .withFileName(scanDocument.getFileName())
                        .withManualIntervention(scanDocument.getManualIntervention())
                        .withNotes(scanDocument.getNotes())
                        .withProsecutorAuthorityCode(scanDocument.getProsecutorAuthorityCode())
                        .withProsecutorAuthorityId(scanDocument.getProsecutorAuthorityId())
                        .withScanDocumentId(scanDocument.getScanDocumentId())
                        .withScanningDate(scanDocument.getScanningDate())
                        .withStatusUpdatedDate(scanDocument.getStatusUpdatedDate())
                        .withVendorReceivedDate(scanDocument.getVendorReceivedDate())
                        .withSource(Source.BULKSCAN)
                        .withZipFileName(scanDocument.getZipFileName())
                        .withType(getDocumentType(scanDocument.getDocumentName()))
                        .withStatus(OUTSTANDING)
                        .build()).build();
    }

    public ReceiveOutstandingDocument asOutstandingDocument(final ScanEnvelopeDocument document) {

        return receiveOutstandingDocument().withOutstandingDocument(
                document()
                        .withActionedBy(document.getActionedBy())
                        .withCasePTIUrn(document.getCasePTIUrn())
                        .withCaseUrn(document.getCaseUrn())
                        .withFileName(document.getDocumentFileName())
                        .withDocumentName(document.getDocumentName())
                        .withNotes(document.getNotes())
                        .withProsecutorAuthorityCode(document.getProsecutorAuthorityCode())
                        .withProsecutorAuthorityId(document.getProsecutorAuthorityId())
                        .withScanDocumentId(document.getId())
                        .withStatusUpdatedDate(document.getStatusUpdatedDate())
                        .withVendorReceivedDate(document.getVendorReceivedDate())
                        .withSource(Source.BULKSCAN)
                        .withZipFileName(document.getZipFileName())
                        .withEnvelopeId(document.getScanEnvelopeId().toString())
                        .withType(getDocumentType(document.getDocumentName()))
                        .withStatus(OUTSTANDING)
                        .withType(getDocumentType(document.getDocumentName()))
                        .withStatusCode(document.getStatusCode())
                        .build()).build();
    }

    private Type getDocumentType(String documentName) {
        if (documentName.contains("Single Justice Procedure Notice - Plea (Multiple)")  || documentName.contains("Single Justice Procedure Notice - Plea (Single)")) {
            return Type.PLEA;
        }

        if (documentName.contains("Application for extension of precharge bail")) {
            return Type.APPLICATION;
        }

        if (documentName.contains("return to sender envelope")) {
            return Type.CORRESPONDENCE;
        } else {
            return Type.OTHER;
        }
    }
}
