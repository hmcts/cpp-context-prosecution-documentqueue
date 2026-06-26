package uk.gov.moj.cpp.prosecution.documentqueue.event.processor;


import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.documentqueue.service.SystemIdMapperService.SOURCE_TYPE;
import static uk.gov.moj.cpp.prosecution.documentqueue.service.SystemIdMapperService.SPI_SOURCE_TYPE;
import static uk.gov.moj.cpp.prosecution.documentqueue.service.SystemIdMapperService.SPI_TARGET_TYPE;
import static uk.gov.moj.cpp.prosecution.documentqueue.service.SystemIdMapperService.TARGET_TYPE;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.prosecution.documentqueue.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemIdMapperServiceTest {

    @InjectMocks
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private SystemIdMapperClient systemIdMapperClient;

    @Test
    public void shouldReturnCaseIdWhenCaseIdMappingExists() {

        final String caseURN = "WILED2603123456";
        final String OU_CODE = "GARWL00";
        final String prosecutorCaseReference = OU_CODE + ":" + caseURN;
        final UUID userId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), caseURN, "", mappedCppCaseId, "", now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(prosecutorCaseReference, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(prosecutorCaseReference, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(caseURN, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.of(systemIdMapping));

        final UUID cppCaseId = systemIdMapperService.getCppCaseIdFor(prosecutorCaseReference);

        assertThat(cppCaseId, is(mappedCppCaseId));
    }


}
