package uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata.RefDataHelper.asDocumentsMetadataRefData;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.documentqueue.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata.pojo.Prosecutor;

import javax.inject.Inject;
import javax.json.JsonValue;

public class ReferenceDataService implements ReferenceDataServiceInterface {

    public static final String REFERENCEDATA_QUERY_GET_PROSECUTOR = "referencedata.query.get.prosecutor.by.oucode";
    private static final String REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE = "referencedata.query.document-type-access-by-sectioncode";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Override
    public String getAuthorityShortNameForOUCode(final String ouCode) {
        final Metadata metadata = metadataBuilder().withName(REFERENCEDATA_QUERY_GET_PROSECUTOR)
                .withId(randomUUID())
                .build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add("oucode", ouCode).build());
        return requester.requestAsAdmin(envelope, Prosecutor.class).payload().getShortName();

    }

    @Override
    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public DocumentTypeAccessReferenceData getDocumentTypeAccessBySectionCode(final Metadata metadata, final String sectionCode) {


        final JsonEnvelope documentTypeAccessBySectionCodeEnvelope = JsonEnvelope.envelopeFrom(metadataFrom(metadata)
                        .withName(REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE),
                createObjectBuilder().
                        add("sectionCode", sectionCode).build());

        final JsonValue response = requester.requestAsAdmin(documentTypeAccessBySectionCodeEnvelope).payload();

        DocumentTypeAccessReferenceData documentTypeAccessReferenceData = null;
        if (null != response && JsonValue.NULL != response) {
            documentTypeAccessReferenceData = asDocumentsMetadataRefData().apply(response);
        }

        return documentTypeAccessReferenceData;
    }

}
