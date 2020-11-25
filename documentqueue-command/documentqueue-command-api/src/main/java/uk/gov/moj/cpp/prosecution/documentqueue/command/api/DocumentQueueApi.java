package uk.gov.moj.cpp.prosecution.documentqueue.command.api;


import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class DocumentQueueApi {

    @Inject
    private Sender sender;

    @Handles("documentqueue.update-document-status")
    public void updateDocumentStatus(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payload())
                .withName("documentqueue.command.update-document-status")
                .withMetadataFrom(envelope));
    }

    @Handles("documentqueue.attach-document")
    public void attachDocument(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payload())
                .withName("documentqueue.command.attach-document")
                .withMetadataFrom(envelope));
    }

    @Handles("documentqueue.delete-documents-of-cases")
    public void deleteDocumentsOfCases(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payload())
                .withName("documentqueue.command.delete-documents-of-cases")
                .withMetadataFrom(envelope));
    }

    @Handles("documentqueue.delete-expired-documents")
    public void deleteExpiredDocuments(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payload())
                .withName("documentqueue.command.delete-expired-documents")
                .withMetadataFrom(envelope));
    }

}
