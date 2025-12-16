package uk.gov.moj.cpp.prosecution.documentqueue.event.converter;

import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.json.JsonObject;

public class DocumentConverter {

    public Document convertDocument(final JsonObject payload, final Optional<ZonedDateTime> eventDateTime) {
        final Document.DocumentBuilder documentBuilder = new Document.DocumentBuilder();

        getUUID(payload, "scanDocumentId").ifPresent(documentBuilder::withId);
        getString(payload, "caseUrn").ifPresent(documentBuilder::withCaseUrn);
        getString(payload, "casePTIUrn").ifPresent(documentBuilder::withCasePTIUrn);
        getString(payload, "prosecutorAuthorityId").ifPresent(documentBuilder::withProsecutorAuthorityId);
        getString(payload, "prosecutorAuthorityCode").ifPresent(documentBuilder::withProsecutorAuthorityCode);
        getString(payload, "documentControlNumber").ifPresent(documentBuilder::withDocumentControlNumber);
        getString(payload, "documentName").ifPresent(documentBuilder::withDocumentName);
        getString(payload, "scanningDate").map(ZonedDateTime::parse).ifPresent(documentBuilder::withScanningDate);
        getString(payload, "manualIntervention").ifPresent(documentBuilder::withManualIntervention);
        getUUID(payload, "envelopeId").ifPresent(documentBuilder::withEnvelopeId);
        getString(payload, "source").map(Source::valueOf).ifPresent(documentBuilder::withSource);
        getString(payload, "fileName").ifPresent(documentBuilder::withFileName);
        getString(payload, "zipFileName").ifPresent(documentBuilder::withZipFileName);
        getString(payload, "notes").ifPresent(documentBuilder::withNotes);
        getString(payload, "vendorReceivedDate").map(ZonedDateTime::parse).ifPresent(documentBuilder::withVendorReceivedDate);
        getString(payload, "status").map(Status::valueOf).ifPresent(documentBuilder::withStatus);
        getString(payload, "type").map(Type::valueOf).ifPresent(documentBuilder::withType);
        getString(payload, "statusUpdatedDate").map(ZonedDateTime::parse).ifPresent(documentBuilder::withStatusUpdatedDate);
        getUUID(payload, "userId").ifPresent(documentBuilder::withUserId);
        getUUID(payload, "actionedBy").ifPresent(documentBuilder::withActionedBy);
        getUUID(payload, "materialId").ifPresent(documentBuilder::withMaterialId);
        getUUID(payload, "fileServiceId").ifPresent(documentBuilder::withFileServiceId);
        getString(payload,"receivedDateTime").map(ZonedDateTime::parse).ifPresent(documentBuilder::withReceivedDateTime);
        getString(payload,"externalDocumentId").ifPresent(documentBuilder::withExternalDocumentId);
        getString(payload, "statusCode").ifPresent(documentBuilder::withStatusCode);
        getUUID(payload, "caseId").ifPresent(documentBuilder::withCaseId);

        if(eventDateTime.isPresent()) {
            documentBuilder.withLastModified(eventDateTime.get());
        } else {
            final Optional<ZonedDateTime> lastModified = getString(payload, "lastModified").map(ZonedDateTime::parse);
            lastModified.ifPresent(documentBuilder::withLastModified);
        }

        return documentBuilder.build();
    }

}
