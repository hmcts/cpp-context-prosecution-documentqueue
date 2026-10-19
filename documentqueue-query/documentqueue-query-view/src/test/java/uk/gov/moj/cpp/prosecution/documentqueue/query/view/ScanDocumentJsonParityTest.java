package uk.gov.moj.cpp.prosecution.documentqueue.query.view;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.prosecution.documentqueue.domain.model.ScanDocument;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master), DQ-M04 / BC-08.
 *
 * The document queries return ScanDocument, whose ZonedDateTime fields the framework mapper writes. Jackson moves
 * from 2.12 to 2.21, and Hibernate 6 can hand back the same instant in a different zone (UTC region, ZoneOffset.UTC
 * or the stored offset), so pin the written string for each of those zone shapes.
 */
class ScanDocumentJsonParityTest {

    private final ObjectToJsonObjectConverter converter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Test
    void dqM04_shouldWriteScanDocumentDatesInTheJ17Format() {
        final ScanDocument scanDocument = ScanDocument.scanDocument()
                .withDocumentId(UUID.fromString("2b0f6f6e-3c7a-4f3e-9d0e-6d2b8a1c4e51"))
                .withReceivedDate(ZonedDateTime.of(2024, 6, 1, 10, 0, 0, 0, ZoneId.of("Europe/London")))
                .withStatusUpdatedDate(ZonedDateTime.of(2024, 6, 1, 9, 0, 0, 123_456_000, ZoneId.of("UTC")))
                .withHearingDate(ZonedDateTime.of(2024, 6, 1, 9, 0, 0, 0, ZoneOffset.UTC))
                .build();

        final JsonObject json = converter.convert(scanDocument);

        assertThat(json.getString("receivedDate") + " " + json.getString("statusUpdatedDate") + " " + json.getString("hearingDate"),
                is("2024-06-01T09:00:00.000Z 2024-06-01T09:00:00.123Z 2024-06-01T09:00:00.000Z"));
    }
}
