package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.factory;

import static java.lang.String.format;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database.DBUtil.waitUntilDataPersist;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.EventName.USERSGROUPS_EVENTS_USER_CREATED;
import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms.SimpleMessageQueueClient.publishPrivateEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserFactory {

    private static final String ID_CHECK = "id = '%s'";

    public static void newUser(final String userId) throws IOException {
        final Map<String, String> values = new HashMap<>();
        publishPrivateEvent(USERSGROUPS_EVENTS_USER_CREATED, "stub-data/user-created.json", values);
        waitUntilDataPersist("cpp_user", format(ID_CHECK, userId), 1);
    }
}
