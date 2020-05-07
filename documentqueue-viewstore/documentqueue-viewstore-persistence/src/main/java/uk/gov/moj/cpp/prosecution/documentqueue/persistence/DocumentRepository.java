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
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DocumentRepository extends EntityRepository<Document, UUID> {

    List<Document> findBySourceAndStatusOrderByVendorReceivedDateAsc(final Source source, final Status status);

    List<Document> findByStatusOrderByVendorReceivedDateAsc(final Status status);

    @Query(value = "SELECT new uk.gov.moj.cpp.prosecution.documentqueue.mapping.DocumentCountMapping(COUNT(*) AS count, source, status, type) FROM Document GROUP BY type, status, source")
    List<DocumentCountMapping> getDocumentCount();

    List<Document> findByStatusNotEqualOrderByVendorReceivedDateAsc(final Status status);

    List<Document> findBySourceAndStatusNotEqualOrderByVendorReceivedDateAsc(final Source source, final Status status);

    List<Document> findByTypeAndStatusNotEqualOrderByVendorReceivedDateAsc(final Type type, final Status status);

    List<Document> findBySourceAndTypeAndStatusNotEqualOrderByVendorReceivedDateAsc(final Source source, final Type type, final Status status);
}
