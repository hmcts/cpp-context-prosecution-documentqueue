package uk.gov.moj.cpp.prosecution.documentqueue.it.test;

import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.cleanDatabase;

import java.util.concurrent.atomic.AtomicBoolean;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.BeforeClass;

public class BaseIT {

    private static AtomicBoolean atomicBoolean = new AtomicBoolean();

    @BeforeClass
    public static void setupOnce() throws Throwable {
        if (!atomicBoolean.get()) {
            atomicBoolean.set(true);

            WireMock.configureFor(System.getProperty("INTEGRATION_HOST_KEY", "localhost"), 8080);
            WireMock.reset();
        }
    }

    @Before
    public void setUpBaseTest() {
        cleanDatabase();
    }
}
