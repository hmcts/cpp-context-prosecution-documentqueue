package uk.gov.moj.cpp.prosecution.documentqueue.persistence;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.documentqueue.entity.Document;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the construction of the PostgreSQL-specific native queries. These use interval
 * arithmetic that the H2 test database cannot prepare, so they are exercised here against a
 * mocked {@link EntityManager} rather than through the Hibernate test persistence unit.
 */
@ExtendWith(MockitoExtension.class)
public class DocumentRepositoryNativeQueryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private DocumentRepository documentRepository;

    @BeforeEach
    void createRepositoryWithMockedEntityManager() {
        documentRepository = new DocumentRepository();
        documentRepository.entityManager = entityManager;
    }

    @Test
    public void shouldBuildExpiredDocumentsNativeQuery() {

        final List<Document> expected = emptyList();

        when(entityManager.createNativeQuery(contains("d.status in ('IN_PROGRESS', 'OUTSTANDING')"), eq(Document.class))).thenReturn(query);
        when(query.setParameter("documentExpiryDays", 7)).thenReturn(query);
        when(query.getResultList()).thenReturn(expected);

        final List<Document> result = documentRepository.getExpiredDocuments(7);

        assertThat(result, is(expected));
        verify(query).setParameter("documentExpiryDays", 7);
        verify(query).getResultList();
    }

    @Test
    public void shouldBuildDocumentsEligibleForDeletionNativeQuery() {

        final List<Document> expected = emptyList();

        when(entityManager.createNativeQuery(contains("d.status in ('COMPLETED', 'DELETED')"), eq(Document.class))).thenReturn(query);
        when(query.setParameter("days", 30)).thenReturn(query);
        when(query.setParameter("maxResults", 100)).thenReturn(query);
        when(query.getResultList()).thenReturn(expected);

        final List<Document> result = documentRepository.getDocumentsEligibleForDeletionFromFileStore(30, 100);

        assertThat(result, is(expected));
        verify(query).setParameter("days", 30);
        verify(query).setParameter("maxResults", 100);
        verify(query).getResultList();
    }
}
