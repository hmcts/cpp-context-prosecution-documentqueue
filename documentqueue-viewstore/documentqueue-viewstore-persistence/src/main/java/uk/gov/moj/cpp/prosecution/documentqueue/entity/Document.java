package uk.gov.moj.cpp.prosecution.documentqueue.entity;

import static java.util.Optional.ofNullable;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "document")
public class Document implements Serializable {

    @Id
    @Column(name = "id")
    private UUID scanDocumentId;

    @Column(name = "case_urn")
    private String caseUrn;

    @Column(name = "case_pti_urn")
    private String casePTIUrn;

    @Column(name = "prosecutor_authority_id")
    private String prosecutorAuthorityId;

    @Column(name = "prosecutor_authority_code")
    private String prosecutorAuthorityCode;

    @Column(name = "document_control_number")
    private String documentControlNumber;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "scanning_date_ts")
    private ZonedDateTime scanningDate;

    @Column(name = "manual_intervention")
    private String manualIntervention;

    @Column(name = "envelope_id")
    private UUID envelopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private Source source;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "zip_file_name")
    private String zipFileName;

    @Column(name = "notes")
    private String notes;

    @Column(name = "vendor_received_date_ts")
    private ZonedDateTime vendorReceivedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Type type;

    @Column(name = "status_updated_date_ts")
    private ZonedDateTime statusUpdatedDate;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "actioned_by")
    private UUID actionedBy;

    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "last_modified_ts")
    private ZonedDateTime lastModified;

    @Column(name = "file_service_id")
    private UUID fileServiceId;

    @Column(name = "external_document_id")
    private String externalDocumentId;

    @Column(name = "received_date_time")
    private ZonedDateTime receivedDateTime;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "case_id")
    private UUID caseId;

    public Document() {
    }

    public Document(final DocumentBuilder documentBuilder) {
        this.scanDocumentId = documentBuilder.id;
        this.casePTIUrn = documentBuilder.casePTIUrn;
        this.caseUrn = documentBuilder.caseUrn;
        this.documentControlNumber = documentBuilder.documentControlNumber;
        this.documentName = documentBuilder.documentName;
        this.envelopeId = documentBuilder.envelopeId;
        this.manualIntervention = documentBuilder.manualIntervention;
        this.prosecutorAuthorityCode = documentBuilder.prosecutorAuthorityCode;
        this.prosecutorAuthorityId = documentBuilder.prosecutorAuthorityId;
        this.scanningDate = documentBuilder.scanningDate;
        this.actionedBy = documentBuilder.actionedBy;
        this.fileName = documentBuilder.fileName;
        this.materialId = documentBuilder.materialId;
        this.notes = documentBuilder.notes;
        this.source = documentBuilder.source;
        this.status = documentBuilder.status;
        this.statusUpdatedDate = documentBuilder.statusUpdatedDate;
        this.type = documentBuilder.type;
        this.userId = documentBuilder.userId;
        this.vendorReceivedDate = documentBuilder.vendorReceivedDate;
        this.zipFileName = documentBuilder.zipFileName;
        this.lastModified = documentBuilder.lastModified;
        this.fileServiceId = documentBuilder.fileServiceId;
        this.receivedDateTime = documentBuilder.receivedDateTime;
        this.externalDocumentId = documentBuilder.externalDocumentId;
        this.statusCode = documentBuilder.statusCode;
        this.caseId = documentBuilder.caseId;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public String getCasePTIUrn() {
        return casePTIUrn;
    }

    public String getProsecutorAuthorityId() {
        return prosecutorAuthorityId;
    }

    public String getProsecutorAuthorityCode() {
        return prosecutorAuthorityCode;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public String getDocumentName() {
        return documentName;
    }

    public ZonedDateTime getScanningDate() {
        return scanningDate;
    }

    public String getManualIntervention() {
        return manualIntervention;
    }

    public UUID getEnvelopeId() {
        return envelopeId;
    }

    public Source getSource() {
        return source;
    }

    public String getFileName() {
        return fileName;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public String getNotes() {
        return notes;
    }

    public ZonedDateTime getVendorReceivedDate() {
        return vendorReceivedDate;
    }

    public Status getStatus() {
        return status;
    }

    public Type getType() {
        return type;
    }

    public ZonedDateTime getStatusUpdatedDate() {
        return statusUpdatedDate;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getActionedBy() {
        return actionedBy;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public ZonedDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(final ZonedDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    public String getExternalDocumentId() {
        return externalDocumentId;
    }

    public ZonedDateTime getReceivedDateTime() {
        return receivedDateTime;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public static class DocumentBuilder {
        private UUID id;
        private String caseUrn;
        private String casePTIUrn;
        private String prosecutorAuthorityId;
        private String prosecutorAuthorityCode;
        private String documentControlNumber;
        private String documentName;
        private ZonedDateTime scanningDate;
        private String manualIntervention;
        private UUID envelopeId;
        private Source source;
        private String fileName;
        private String zipFileName;
        private String notes;
        private ZonedDateTime vendorReceivedDate;
        private Status status;
        private Type type;
        private ZonedDateTime statusUpdatedDate;
        private UUID userId;
        private UUID actionedBy;
        private UUID materialId;
        private ZonedDateTime lastModified;
        private UUID fileServiceId;
        private String externalDocumentId;
        private ZonedDateTime receivedDateTime;
        private String statusCode;
        private UUID caseId;

        public static DocumentBuilder document() {
            return new DocumentBuilder();
        }

        public Document build() {
            return new Document(this);
        }

        @SuppressWarnings({"squid:S1188"})
        public DocumentBuilder withDocument(final Document document) {
            ofNullable(document).ifPresent(doc -> {
                withId(doc.getScanDocumentId());
                withCaseUrn(doc.getCaseUrn());
                withCasePTIUrn(doc.getCasePTIUrn());
                withProsecutorAuthorityId(doc.getProsecutorAuthorityId());
                withProsecutorAuthorityCode(doc.getProsecutorAuthorityCode());
                withDocumentControlNumber(doc.getDocumentControlNumber());
                withDocumentName(doc.getDocumentName());
                withScanningDate(doc.getScanningDate());
                withManualIntervention(doc.getManualIntervention());
                withEnvelopeId(doc.getEnvelopeId());
                withSource(doc.getSource());
                withFileName(doc.getFileName());
                withZipFileName(doc.getZipFileName());
                withNotes(doc.getNotes());
                withVendorReceivedDate(doc.getVendorReceivedDate());
                withStatus(doc.getStatus());
                withType(doc.getType());
                withStatusUpdatedDate(doc.getStatusUpdatedDate());
                withUserId(doc.getUserId());
                withActionedBy(doc.getActionedBy());
                withMaterialId(doc.getMaterialId());
                withLastModified(doc.getLastModified());
                withFileServiceId(doc.getFileServiceId());
                withExternalDocumentId(doc.getExternalDocumentId());
                withReceivedDateTime(doc.getReceivedDateTime());
                withStatusCode(doc.getStatusCode());
                withCaseId(doc.getCaseId());
            });
            return this;
        }

        public DocumentBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public DocumentBuilder withCaseUrn(final String caseUrn) {
            this.caseUrn = caseUrn;
            return this;
        }

        public DocumentBuilder withCasePTIUrn(final String casePTIUrn) {
            this.casePTIUrn = casePTIUrn;
            return this;
        }

        public DocumentBuilder withProsecutorAuthorityId(final String prosecutorAuthorityId) {
            this.prosecutorAuthorityId = prosecutorAuthorityId;
            return this;
        }

        public DocumentBuilder withProsecutorAuthorityCode(final String prosecutorAuthorityCode) {
            this.prosecutorAuthorityCode = prosecutorAuthorityCode;
            return this;
        }

        public DocumentBuilder withDocumentControlNumber(final String documentControlNumber) {
            this.documentControlNumber = documentControlNumber;
            return this;
        }

        public DocumentBuilder withDocumentName(final String documentName) {
            this.documentName = documentName;
            return this;
        }

        public DocumentBuilder withScanningDate(final ZonedDateTime scanningDate) {
            this.scanningDate = scanningDate;
            return this;
        }

        public DocumentBuilder withManualIntervention(final String manualIntervention) {
            this.manualIntervention = manualIntervention;
            return this;
        }

        public DocumentBuilder withEnvelopeId(final UUID envelopeId) {
            this.envelopeId = envelopeId;
            return this;
        }

        public DocumentBuilder withSource(final Source source) {
            this.source = source;
            return this;
        }

        public DocumentBuilder withFileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public DocumentBuilder withZipFileName(final String zipFileName) {
            this.zipFileName = zipFileName;
            return this;
        }

        public DocumentBuilder withNotes(final String notes) {
            this.notes = notes;
            return this;
        }

        public DocumentBuilder withVendorReceivedDate(final ZonedDateTime vendorReceivedDate) {
            this.vendorReceivedDate = vendorReceivedDate;
            return this;
        }

        public DocumentBuilder withStatus(final Status status) {
            this.status = status;
            return this;
        }

        public DocumentBuilder withType(final Type type) {
            this.type = type;
            return this;
        }

        public DocumentBuilder withStatusUpdatedDate(final ZonedDateTime statusUpdatedDate) {
            this.statusUpdatedDate = statusUpdatedDate;
            return this;
        }

        public DocumentBuilder withUserId(final UUID userId) {
            this.userId = userId;
            return this;
        }

        public DocumentBuilder withActionedBy(final UUID actionedBy) {
            this.actionedBy = actionedBy;
            return this;
        }

        public DocumentBuilder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public DocumentBuilder withLastModified(final ZonedDateTime lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public DocumentBuilder withFileServiceId(final UUID fileServiceId) {
            this.fileServiceId = fileServiceId;
            return this;
        }

        public DocumentBuilder withExternalDocumentId(final String externalDocumentId) {
            this.externalDocumentId = externalDocumentId;
            return this;
        }

        public DocumentBuilder withReceivedDateTime(final ZonedDateTime receivedDateTime) {
            this.receivedDateTime = receivedDateTime;
            return this;
        }

        public DocumentBuilder withStatusCode(final String statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public DocumentBuilder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }
    }
}
