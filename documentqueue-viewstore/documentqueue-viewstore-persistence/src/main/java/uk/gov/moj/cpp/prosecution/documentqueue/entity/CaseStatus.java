package uk.gov.moj.cpp.prosecution.documentqueue.entity;

import static java.util.Optional.ofNullable;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "case_status")
public class CaseStatus implements Serializable {


    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "status")
    private Status status;

    public CaseStatus() {
    }

    public CaseStatus(final CaseStatusBuilder caseStatusBuilder) {
        this.id = caseStatusBuilder.id;
        this.caseId = caseStatusBuilder.caseId;
        this.status = caseStatusBuilder.status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Status getStatus() {
        return status;
    }


    public static class CaseStatusBuilder {
        private UUID id;
        private UUID caseId;
        private Status status;


        public static CaseStatusBuilder caseStatus() {
            return new CaseStatusBuilder();
        }

        public CaseStatus build() {
            return new CaseStatus(this);
        }

        @SuppressWarnings({"squid:S1188"})
        public CaseStatusBuilder withCaseStatus(final CaseStatus caseStatus) {
            ofNullable(caseStatus).ifPresent(obj -> {
                withCaseId(obj.getCaseId());
                withStatus(obj.getStatus());
            });
            return this;
        }

        public CaseStatusBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public CaseStatusBuilder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }



        public CaseStatusBuilder withStatus(final Status status) {
            this.status = status;
            return this;
        }


    }
}
