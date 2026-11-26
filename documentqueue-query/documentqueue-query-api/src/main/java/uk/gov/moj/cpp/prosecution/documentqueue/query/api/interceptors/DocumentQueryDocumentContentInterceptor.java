package uk.gov.moj.cpp.prosecution.documentqueue.query.api.interceptors;

import uk.gov.justice.services.components.query.api.interceptors.DefaultQueryApiInterceptorProvider;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;

import javax.enterprise.inject.Specializes;


@Specializes
public class DocumentQueryDocumentContentInterceptor extends DefaultQueryApiInterceptorProvider {

    @Override
    public InterceptorChainEntry createAuditInterceptorEntry(int priority) {
        return new InterceptorChainEntry(priority, DocumentQueueAuditInterceptor.class);
    }

}