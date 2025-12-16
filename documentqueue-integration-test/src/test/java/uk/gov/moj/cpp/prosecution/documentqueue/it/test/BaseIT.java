package uk.gov.moj.cpp.prosecution.documentqueue.it.test;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.cleanDatabase;

@ExtendWith(JmsResourceManagementExtension.class)
public class BaseIT {

    private static AtomicBoolean atomicBoolean = new AtomicBoolean();

    @BeforeAll
    public static void setupOnce() throws Throwable {
        if (!atomicBoolean.get()) {
            atomicBoolean.set(true);

            WireMock.configureFor(System.getProperty("INTEGRATION_HOST_KEY", "localhost"), 8080);
            WireMock.reset();
        }
    }

    @BeforeEach
    public void setUpBaseTest() {
        cleanDatabase();
    }
}
