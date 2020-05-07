package uk.gov.moj.cpp.prosecution.documentqueue.event.matcher;

import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ScanDocumentMatcher extends TypeSafeMatcher<Document> {

    private final Document scanDocument;

    private ScanDocumentMatcher(final Document scanDocument) {
        this.scanDocument = scanDocument;
    }

    @Override
    protected boolean matchesSafely(final Document document) {
        return matchesDocumentId(document) && matchesActionedBy(document) && matchesCaseId(document) && matchesCasePTIURN(document) &&
                matchesCaseUrn(document) && matchesDocumentControlNumber(document) && matchesDocumentName(document) && matchesEnvelopeId(document) &&
                matchesExternalDocumentId(document) && matchesFileName(document) && matchesFileServiceId(document) && matchesLastModified(document) &&
                matchesManualIntervention(document) && matchesMaterialId(document) && matchesNotes(document) && matchesProsecutorAuthorityCode(document) &&
                matchesProsecutorAuthorityId(document) && matchesReceivedDateTime(document) && matchesScanningDate(document) && matchesSource(document) &&
                matchesStatus(document) && matchesStatusCode(document) && matchesStatusUpdatedDate(document) && matchesType(document) && matchesUserId(document) &&
                matchesVendorReceivedDate(document) && matchesZipFileName(document);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("Document not matched");
    }

    public static ScanDocumentMatcher matchesNonNullPropertiesOfDocument(final Document document) {
        return new ScanDocumentMatcher(document);
    }

    private boolean matchesDocumentId(final Document document) {
        if (scanDocument.getScanDocumentId() != null) {
            return scanDocument.getScanDocumentId().equals(document.getScanDocumentId());
        }
        return true;
    }

    private boolean matchesCaseUrn(final Document document) {
        if (scanDocument.getCaseUrn() != null) {
            return scanDocument.getCaseUrn().equals(document.getCaseUrn());
        }
        return true;
    }

    private boolean matchesCasePTIURN(final Document document) {
        if (scanDocument.getCasePTIUrn() != null) {
            return scanDocument.getCasePTIUrn().equals(document.getCasePTIUrn());
        }
        return true;
    }

    private boolean matchesProsecutorAuthorityId(final Document document) {
        if (scanDocument.getProsecutorAuthorityId() != null) {
            return scanDocument.getProsecutorAuthorityId().equals(document.getProsecutorAuthorityId());
        }
        return true;
    }

    private boolean matchesProsecutorAuthorityCode(final Document document) {
        if (scanDocument.getProsecutorAuthorityCode() != null) {
            return scanDocument.getProsecutorAuthorityCode().equals(document.getProsecutorAuthorityCode());
        }
        return true;
    }

    private boolean matchesDocumentControlNumber(final Document document) {
        if (scanDocument.getDocumentControlNumber() != null) {
            return scanDocument.getDocumentControlNumber().equals(document.getDocumentControlNumber());
        }
        return true;
    }

    private boolean matchesDocumentName(final Document document) {
        if (scanDocument.getDocumentName() != null) {
            return scanDocument.getDocumentName().equals(document.getDocumentName());
        }
        return true;
    }

    private boolean matchesScanningDate(final Document document) {
        if (scanDocument.getScanningDate() != null) {
            return scanDocument.getScanningDate().equals(document.getScanningDate());
        }
        return true;
    }

    private boolean matchesManualIntervention(final Document document) {
        if (scanDocument.getManualIntervention() != null) {
            return scanDocument.getManualIntervention().equals(document.getManualIntervention());
        }
        return true;
    }

    private boolean matchesEnvelopeId(final Document document) {
        if (scanDocument.getEnvelopeId() != null) {
            return scanDocument.getEnvelopeId().equals(document.getEnvelopeId());
        }
        return true;
    }

    private boolean matchesSource(final Document document) {
        if (scanDocument.getSource() != null) {
            return scanDocument.getSource().equals(document.getSource());
        }
        return true;
    }

    private boolean matchesFileName(final Document document) {
        if (scanDocument.getFileName() != null) {
            return scanDocument.getFileName().equals(document.getFileName());
        }
        return true;
    }

    private boolean matchesNotes(final Document document) {
        if (scanDocument.getNotes() != null) {
            return scanDocument.getNotes().equals(document.getNotes());
        }
        return true;
    }

    private boolean matchesVendorReceivedDate(final Document document) {
        if (scanDocument.getVendorReceivedDate() != null) {
            return scanDocument.getVendorReceivedDate().equals(document.getVendorReceivedDate());
        }
        return true;
    }

    private boolean matchesZipFileName(final Document document) {
        if (scanDocument.getZipFileName() != null) {
            return scanDocument.getZipFileName().equals(document.getZipFileName());
        }
        return true;
    }

    private boolean matchesStatus(final Document document) {
        if (scanDocument.getStatus() != null) {
            return scanDocument.getStatus().equals(document.getStatus());
        }
        return true;
    }

    private boolean matchesType(final Document document) {
        if (scanDocument.getType() != null) {
            return scanDocument.getType().equals(document.getType());
        }
        return true;
    }

    private boolean matchesStatusUpdatedDate(final Document document) {
        if (scanDocument.getStatusUpdatedDate() != null) {
            return scanDocument.getStatusUpdatedDate().equals(document.getStatusUpdatedDate());
        }
        return true;
    }

    private boolean matchesUserId(final Document document) {
        if (scanDocument.getUserId() != null) {
            return scanDocument.getUserId().equals(document.getUserId());
        }
        return true;
    }

    private boolean matchesActionedBy(final Document document) {
        if (scanDocument.getActionedBy() != null) {
            return scanDocument.getActionedBy().equals(document.getActionedBy());
        }
        return true;
    }

    private boolean matchesMaterialId(final Document document) {
        if (scanDocument.getMaterialId() != null) {
            return scanDocument.getMaterialId().equals(document.getMaterialId());
        }
        return true;
    }

    private boolean matchesFileServiceId(final Document document) {
        if (scanDocument.getFileServiceId() != null) {
            return scanDocument.getFileServiceId().equals(document.getFileServiceId());
        }
        return true;
    }

    private boolean matchesExternalDocumentId(final Document document) {
        if (scanDocument.getExternalDocumentId() != null) {
            return scanDocument.getExternalDocumentId().equals(document.getExternalDocumentId());
        }
        return true;
    }

    private boolean matchesReceivedDateTime(final Document document) {
        if (scanDocument.getReceivedDateTime() != null) {
            return scanDocument.getReceivedDateTime().equals(document.getReceivedDateTime());
        }
        return true;
    }

    private boolean matchesCaseId(final Document document) {
        if (scanDocument.getCaseId() != null) {
            return scanDocument.getCaseId().equals(document.getCaseId());
        }
        return true;
    }

    private boolean matchesStatusCode(final Document document) {
        if (scanDocument.getStatusCode() != null) {
            return scanDocument.getStatusCode().equals(document.getStatusCode());
        }
        return true;
    }

    private boolean matchesLastModified(final Document document) {
        if (scanDocument.getLastModified() != null) {
            return scanDocument.getLastModified().equals(document.getLastModified());
        }
        return true;
    }
}
