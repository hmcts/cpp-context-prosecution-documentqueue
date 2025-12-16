package uk.gov.moj.cpp.prosecution.documentqueue.mapping;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;

public class DocumentCountMapping {

    private Long count;

    private Source source;

    private Status status;

    private Type type;

    public DocumentCountMapping(final Long count, final Source source, final Status status, final Type type) {
        this.count = count;
        this.source = source;
        this.status = status;
        this.type = type;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }
}
