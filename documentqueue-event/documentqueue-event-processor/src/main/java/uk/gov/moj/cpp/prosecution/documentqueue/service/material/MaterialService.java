package uk.gov.moj.cpp.prosecution.documentqueue.service.material;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.documentqueue.service.material.pojo.MaterialMetadata;

import java.util.UUID;

import javax.inject.Inject;

public class MaterialService {

    private static final String MATERIAL_METADATA_CONTENT_TYPE = "material.query.material-metadata";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public MaterialMetadata materialMetaDataForMaterialId(final UUID materialId) {

        final Metadata metadata = metadataBuilder().withName(MATERIAL_METADATA_CONTENT_TYPE)
                .withId(randomUUID())
                .build();

        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add("materialId",materialId.toString()).build());
        return requester.requestAsAdmin(envelope, MaterialMetadata.class).payload();

    }
}
