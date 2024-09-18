
package uk.gov.moj.cpp.prosecution.documentqueue.it.test.event;

import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentMarkedAsFollowUp;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newOutstandingDocumentReceived;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newScanEnvelopeRegistered;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.OUTSTANDING_DOCUMENT_RECEIVED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_MARKED_FOLLOW_UP;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_SCAN_ENVELOPE_REGISTERED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubForIdMapperSuccess;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.WireMockStubUtils.stubIdMapperReturningExistingAssociation;

import javax.ws.rs.core.Response;
import uk.gov.moj.cpp.prosecution.documentqueue.it.test.BaseIT;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OutstandingDocumentReceivedIT extends BaseIT {

    @BeforeEach
    public  void setUp() {
        stubIdMapperReturningExistingAssociation(UUID.randomUUID());
        stubForIdMapperSuccess(Response.Status.OK);
    }

    @Test
    public void shouldHandleOutstandingDocumentReceivedEvent() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "PLEA", "OUTSTANDING");
        values.put("fileName", OUTSTANDING_DOCUMENT_RECEIVED);
        newOutstandingDocumentReceived(values);
    }

    @Test
    public void shouldHandlePublicScanEnvelopeReqisteredEvent() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "PLEA", "FOLLOW_UP");
        values.put("fileName", PUBLIC_SCAN_ENVELOPE_REGISTERED);
        newScanEnvelopeRegistered(values);
    }

    @Test
    public void shouldHandlePublicDocumentMarkedFollowUpEvent() {
        final Map<String, String> values = newDocumentValues("BULKSCAN", "PLEA", "FOLLOW_UP");
        values.put("fileName", PUBLIC_DOCUMENT_MARKED_FOLLOW_UP);
        newDocumentMarkedAsFollowUp(values);
    }
}
