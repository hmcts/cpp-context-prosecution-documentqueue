package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Type;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;
import uk.gov.moj.cpp.prosecution.documentqueue.mapping.DocumentCountMapping;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DocumentRepository extends EntityRepository<Document, UUID> {

    List<Document> findBySourceAndStatusOrderByVendorReceivedDateAsc(final Source source, final Status status);

    List<Document> findByStatusOrderByVendorReceivedDateAsc(final Status status);

    @Query(value = "SELECT new uk.gov.moj.cpp.prosecution.documentqueue.mapping.DocumentCountMapping(COUNT(*) AS count, source, status, type) FROM Document GROUP BY type, status, source")
    List<DocumentCountMapping> getDocumentCount();

    // NotIn query support is there from 1.9.1 delta spike so for now adding explicit query
    @Query(value = "SELECT d FROM Document d WHERE d.status NOT IN (:statuses) ORDER BY d.vendorReceivedDate asc")
    List<Document> findByStatusNotInOrderByVendorReceivedDateAsc(@QueryParam("statuses")  final List<Status> statuses);

    @Query(value = "SELECT d FROM Document d WHERE d.source = :source and d.status NOT IN (:statuses) ORDER BY d.vendorReceivedDate asc")
    List<Document> findBySourceAndStatusNotInOrderByVendorReceivedDateAsc(@QueryParam("source") final Source source,@QueryParam("statuses") final List<Status> statuses);

    @Query(value = "SELECT d FROM Document d WHERE d.type = :type and d.status NOT IN (:statuses) ORDER BY d.vendorReceivedDate asc")
    List<Document> findByTypeAndStatusNotInOrderByVendorReceivedDateAsc(@QueryParam("type") final Type type, @QueryParam("statuses") final List<Status> statuses);

    @Query(value = "SELECT d FROM Document d WHERE d.source = :source and d.type = :type and d.status NOT IN (:statuses) ORDER BY d.vendorReceivedDate asc")
    List<Document> findBySourceAndTypeAndStatusNotInOrderByVendorReceivedDateAsc(@QueryParam("source") final Source source, @QueryParam("type") final Type type, @QueryParam("statuses") final List<Status> statuses);

    @Query(value = "SELECT d FROM Document d WHERE d.caseUrn IN (:urns) OR d.casePTIUrn IN (:urns) ORDER BY d.casePTIUrn, d.caseUrn")
    List<Document> findByCaseUrnInOrCasePTIUrnInOrderByCaseUrnAsc(@QueryParam("urns") final List<String> urns);

    @Query(value =
            "SELECT d.* FROM document d " +
                    "WHERE d.case_id not in (select cs.case_id from case_status cs) " +
                    "and d.status in ('IN_PROGRESS', 'OUTSTANDING') " +
                    "and d.source = 'CPS' " +
                    "and d.received_date_time < now() - (interval '1' day) * :documentExpiryDays", isNative = true)
    List<Document> getExpiredDocuments(@QueryParam("documentExpiryDays") final int documentExpiryDays);

    @Query(value =
            "SELECT d.* FROM document d " +
                    "WHERE d.case_id not in (select cs.case_id from case_status cs) " +
                    "and d.status in ('COMPLETED', 'DELETED') " +
                    "and d.source = 'CPS' " +
                    "and d.received_date_time < now() - (interval '1' day) * :days " +
                    "order by d.received_date_time asc limit :maxResults", isNative = true)
    List<Document> getDocumentsEligibleForDeletionFromFileStore(@QueryParam("days") final int days, @QueryParam("maxResults") final int maxResults);
}
