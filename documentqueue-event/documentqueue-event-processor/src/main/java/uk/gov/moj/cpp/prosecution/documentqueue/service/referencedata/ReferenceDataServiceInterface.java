package uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.documentqueue.json.schemas.DocumentTypeAccessReferenceData;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public interface ReferenceDataServiceInterface {

    DocumentTypeAccessReferenceData getDocumentTypeAccessBySectionCode(final Metadata metadata, final String sectionCode);

    String getAuthorityShortNameForOUCode(final String ouCode);

}
