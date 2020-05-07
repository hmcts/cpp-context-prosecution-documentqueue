package uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata.pojo.Prosecutor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Mock
    private Requester requester;

    @Mock
    private Envelope envelope;


    @Test
    public void shouldGetShortNameForProsecutingAuthority(){
        final Prosecutor prosecutor = new Prosecutor("test", "fullName");
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        when(envelope.payload()).thenReturn(prosecutor);
        assertThat("shortName does not match", referenceDataService.getAuthorityShortNameForOUCode("ouCode"), is("test"));

    }


}
