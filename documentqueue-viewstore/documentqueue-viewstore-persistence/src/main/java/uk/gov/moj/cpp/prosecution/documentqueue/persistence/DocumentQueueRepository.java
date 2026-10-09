package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import uk.gov.justice.prosecution.documentqueue.domain.enums.Source;
import uk.gov.justice.prosecution.documentqueue.domain.enums.Status;
import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.apache.commons.lang3.tuple.Pair;

@ApplicationScoped
public class DocumentQueueRepository {

    private static final String SOURCE = "source";

    private static final String STATUS = "status";

    @PersistenceContext(unitName = "documentqueue")
    EntityManager entityManager;

    public Document save(final Document document) {
        return entityManager.merge(document);
    }

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
