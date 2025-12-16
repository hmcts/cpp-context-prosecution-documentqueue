package uk.gov.moj.cpp.prosecution.documentqueue.it.test.event;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newDocumentReviewRequired;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory.DocumentFactory.newReviewDocumentValues;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file.FileServiceHelper.create;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.util.EventPayloadUtil.PUBLIC_DOCUMENT_REVIEW_REQUIRED;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CPSDocumentProcessorIT {

    private final static String PDF_MIME_TYPE = "application/pdf";

    @Test
    @SuppressWarnings({"squid:S1607","squid:S2699"})
    public void shouldCreateDocumentReviewRequiredEvent() throws  Exception {
         final UUID fileStoreId  =  uploadFile(PDF_MIME_TYPE);
          final Map<String, String> values = newReviewDocumentValues("CPS","OTHER",fileStoreId);
            values.put("fileName", PUBLIC_DOCUMENT_REVIEW_REQUIRED);
            newDocumentReviewRequired(values);
        }

    private static UUID uploadFile(final String mimeType) throws Exception {
        return create("XVBN22.pdf", mimeType, getSystemResourceAsStream("materials/XVBN22.pdf"));
    }
}
