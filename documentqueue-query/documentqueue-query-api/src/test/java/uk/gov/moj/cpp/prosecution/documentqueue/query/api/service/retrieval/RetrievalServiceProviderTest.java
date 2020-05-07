package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RetrievalServiceProviderTest {

    @Mock
    private FileServiceRetrievalService fileServiceRetrievalService;

    @Mock
    private MaterialRetrievalService materialRetrievalService;

    @InjectMocks
    private RetrievalServiceProvider retrievalServiceProvider;

    @Test
    public void shouldReturnRetrievalSupplierForMaterialId() {

        final UUID materialId = randomUUID();
        final String expectedContent = "base 64 encoded string";

        when(materialRetrievalService.retrieveBase64EncodedContent(materialId)).thenReturn(expectedContent);

        final Supplier<String> materialSupplier = retrievalServiceProvider.provide(null, materialId);

        assertThat(materialSupplier.get(), is(expectedContent));
    }

    @Test
    public void shouldReturnRetrievalSupplierForFileServiceId() {

        final UUID fileServiceId = randomUUID();
        final String expectedContent = "base 64 encoded string";

        when(fileServiceRetrievalService.retrieveBase64EncodedContent(fileServiceId)).thenReturn(expectedContent);

        final Supplier<String> fileServiceSupplier = retrievalServiceProvider.provide(fileServiceId, null);

        assertThat(fileServiceSupplier.get(), is(expectedContent));
    }
}