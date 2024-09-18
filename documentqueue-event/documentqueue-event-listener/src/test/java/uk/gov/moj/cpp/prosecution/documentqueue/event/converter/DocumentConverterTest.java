package uk.gov.moj.cpp.prosecution.documentqueue.event.converter;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Source.BULKSCAN;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Status.OUTSTANDING;
import static uk.gov.justice.prosecution.documentqueue.domain.enums.Type.CORRESPONDENCE;
import static uk.gov.moj.cpp.prosecution.documentqueue.entity.Document.DocumentBuilder.document;
import static uk.gov.moj.cpp.prosecution.documentqueue.event.matcher.ScanDocumentMatcher.matchesNonNullPropertiesOfDocument;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentConverterTest {

    @InjectMocks
    private DocumentConverter documentConverter;

    @Test
    public void shouldConvertPayloadToDocument() throws Exception {

        final ZonedDateTime currentLastModified = new UtcClock().now().truncatedTo(ChronoUnit.MILLIS);
        final ZonedDateTime previousLastModified = currentLastModified.minusDays(2);

        final Document document = document()
                .withId(randomUUID())
                .withCaseUrn("caseUrn")
                .withCasePTIUrn("casePTIUrn")
                .withProsecutorAuthorityId("prosecutorAuthorityId")
                .withProsecutorAuthorityCode("prosecutorAuthorityCode")
                .withDocumentControlNumber("documentControlNumber")
                .withDocumentName("documentName")
                .withScanningDate(new UtcClock().now().truncatedTo(ChronoUnit.MILLIS).minusHours(23))
                .withManualIntervention("manualIntervention")
                .withEnvelopeId(randomUUID())
                .withSource(BULKSCAN)
                .withFileName("fileName")
                .withNotes("notes")
                .withVendorReceivedDate(new UtcClock().now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(73))
                .withZipFileName("zipFileName")
                .withStatus(OUTSTANDING)
                .withType(CORRESPONDENCE)
                .withStatusUpdatedDate(new UtcClock().now().truncatedTo(ChronoUnit.MILLIS).minusHours(25))
                .withUserId(randomUUID())
                .withActionedBy(randomUUID())
                .withMaterialId(randomUUID())
                .withFileServiceId(randomUUID())
                .withExternalDocumentId("externalDocumentId")
                .withReceivedDateTime(new UtcClock().now().truncatedTo(ChronoUnit.MILLIS).minusDays(1))
                .withLastModified(previousLastModified)
                .withCaseId(randomUUID())
                .withStatusCode("statusCode")
                .build();

        final String json = new ObjectMapperProducer().objectMapper().writer()
                .writeValueAsString(document);
        final JsonObject payload = new StringToJsonObjectConverter().convert(json);

        final Document expectedDocument = new Document.DocumentBuilder().withDocument(document)
                .withLastModified(currentLastModified)
                .build();

        final Document convertedDocument = documentConverter.convertDocument(payload, of(currentLastModified));
        assertThat(convertedDocument, matchesNonNullPropertiesOfDocument(expectedDocument));
    }

    @Test
    public void shouldNotUpdateLastModifiedIfNotPresent() throws Exception {

        final ZonedDateTime lastModified = new UtcClock().now().truncatedTo(ChronoUnit.MILLIS);

        final Document document = document()
                .withId(randomUUID())
                .withLastModified(lastModified)
                .build();

        final String json = new ObjectMapperProducer().objectMapper().writer()
                .writeValueAsString(document);
        final JsonObject payload = new StringToJsonObjectConverter().convert(json);

        final Document convertedDocument = documentConverter.convertDocument(payload, empty());

        assertThat(convertedDocument.getScanDocumentId(), is(document.getScanDocumentId()));
        assertThat(convertedDocument.getLastModified(), is(lastModified));
    }

    @Test
    public void shouldKeepLastModifiedAsNullIfNoneSet() throws Exception {

        final Document document = document()
                .withId(randomUUID())
                .build();

        assertThat(document.getLastModified(), is(nullValue()));

        final String json = new ObjectMapperProducer().objectMapper().writer()
                .writeValueAsString(document);
        final JsonObject payload = new StringToJsonObjectConverter().convert(json);

        final Document convertedDocument = documentConverter.convertDocument(payload, empty());

        assertThat(convertedDocument.getScanDocumentId(), is(document.getScanDocumentId()));
        assertThat(convertedDocument.getLastModified(), is(nullValue()));
    }
}
