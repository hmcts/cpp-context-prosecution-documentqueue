package uk.gov.moj.cpp.prosecution.documentqueue.query.api.service.retrieval;

import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.prosecution.documentqueue.query.api.util.Base64Encoder;

import java.io.InputStream;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialRetrievalServiceTest {

    @Mock
    private MaterialClient materialClient;

    @Mock
    private Base64Encoder base64Encoder;

    @Mock
    private SystemUserProvider systemUserProvider;

    @InjectMocks
    private MaterialRetrievalService materialRetrievalService;

    @Test
    public void shouldGetTheMaterialAndBase64Encoded() {
        final UUID materialId = randomUUID();
        final UUID userId = randomUUID();
        final String base64EncodedResponse = "Base64 Encoded String";

        final Response response = mock(Response.class);
        final InputStream inputStream = mock(InputStream.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));
        when(materialClient.getMaterial(materialId, userId)).thenReturn(response);
        when(response.readEntity(InputStream.class)).thenReturn(inputStream);
        when(base64Encoder.encode(inputStream)).thenReturn(base64EncodedResponse);

        assertThat(materialRetrievalService.retrieveBase64EncodedContent(materialId), is(base64EncodedResponse));
    }

    @Test
    public void shouldFailIfMaterialRetrievalFails() {
        final UUID materialId = randomUUID();
        final UUID userId = randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));
        when(materialClient.getMaterial(materialId, userId)).thenReturn(null);

        try {
            materialRetrievalService.retrieveBase64EncodedContent(materialId);
        } catch (MaterialRetrievalException e) {
            assertThat(e.getMessage(), is(format("Failed to retrieve material by id %s", materialId)));
        }
    }
}