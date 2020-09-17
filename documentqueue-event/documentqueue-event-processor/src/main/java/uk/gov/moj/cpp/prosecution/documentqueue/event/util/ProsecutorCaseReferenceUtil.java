package uk.gov.moj.cpp.prosecution.documentqueue.event.util;

import static java.lang.String.format;
import static java.util.Objects.isNull;

public class ProsecutorCaseReferenceUtil {

    private static final String PROSECUTOR_CASE_PATTERN = "%s:%s";

    private ProsecutorCaseReferenceUtil() {
    }

    public static String getProsecutorCaseReference(final String prosecutingAuthority, final String caseUrn) {
        if (isNull(prosecutingAuthority)) {
            // prosecutingAuthority not available for bulk scanning docs
            return caseUrn;
        }

        return format(PROSECUTOR_CASE_PATTERN, prosecutingAuthority, caseUrn);
    }

}
