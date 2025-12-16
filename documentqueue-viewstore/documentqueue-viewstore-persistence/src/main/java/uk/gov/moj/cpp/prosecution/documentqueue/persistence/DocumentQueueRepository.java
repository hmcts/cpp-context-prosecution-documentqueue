package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;

@Repository
public abstract class DocumentQueueRepository implements EntityRepository<Document, UUID>, CriteriaSupport<Document> {

    private static final String SOURCE = "source";

    private static final String STATUS = "status";

    @Inject
    private EntityManager entityManager;


    public Pair<Integer, List<Document>> getDocumentList(final Optional<Source> source, final Optional<Status> status, final String sort, final String sortOrder, final int offset, final int limit) {

        final CriteriaBuilder qb = entityManager.getCriteriaBuilder();
        final TypedQuery query;
        final CriteriaQuery cq = qb.createQuery();
        final Root<Document> document = cq.from(Document.class);
        final List<Status> statuses = Arrays.asList(Status.DELETED, Status.FILE_DELETED);

        final List<Predicate> predicates = new ArrayList<>();

        if (source.isPresent() && status.isPresent()) {
            predicates.add(qb.equal(document.get(SOURCE), source.get()));
            predicates.add(qb.equal(document.get(STATUS), status.get()));
        } else if (source.isPresent()) {
            predicates.add(qb.equal(document.get(SOURCE), source.get()));
            predicates.add(document.get(STATUS).in(statuses).not());
        } else if (status.isPresent()) {
            predicates.add(qb.equal(document.get(STATUS), status.get()));

        } else {
            predicates.add(document.get(STATUS).in(statuses).not());
        }
        cq.select(document).where(predicates.toArray(new Predicate[]{}));
        if ("asc".equalsIgnoreCase(sortOrder)) {
            cq.orderBy(qb.asc(document.get(sort)));
        } else {
            cq.orderBy(qb.desc(document.get(sort)));
        }

        query = entityManager.createQuery(cq);
        return Pair.of(query.getResultList().size(), query.setFirstResult(offset)
                .setMaxResults(limit).getResultList());
    }
}
