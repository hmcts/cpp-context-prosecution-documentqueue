package uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata;


import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata.pojo.Prosecutor;

import javax.inject.Inject;

public class ReferenceDataService {

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public static final String REFERENCEDATA_QUERY_GET_PROSECUTOR = "referencedata.query.get.prosecutor.by.oucode";

    public String getAuthorityShortNameForOUCode(final String ouCode) {
        final Metadata metadata = metadataBuilder().withName(REFERENCEDATA_QUERY_GET_PROSECUTOR)
                .withId(randomUUID())
                .build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add("oucode", ouCode).build());
        return requester.requestAsAdmin(envelope, Prosecutor.class).payload().getShortName();

    }
}
