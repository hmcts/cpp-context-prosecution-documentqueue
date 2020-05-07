package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms;

import static uk.gov.moj.cpp.prosecution.documentqueue.it.helper.rest.SimpleRestClient.getSubscriptionsFor;

import javax.json.JsonArray;
import javax.json.JsonNumber;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;

public class TopicChecker {
    private TopicChecker() {
    }

    private final static int SUBSCRIPTION_NAME_INDEX = 2;
    private final static int SUBSCRIPTION_MESSAGE_COUNT_INDEX = 4;
    private final static int EMPTY_SUBSCRIPTION_COUNT = 0;
    private final static String SUBSCRIPTION_NAME = "documentqueue";

    public static void waitUntilQueueIsEmpty(final String topicName) {
        Awaitility.await()
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.TWO_HUNDRED_MILLISECONDS)
                .until(() -> {
                            final JsonArray topicSubscriptions = getSubscriptionsFor(topicName).asJsonObject().getJsonArray("value");
                            for (int i = 0; i < topicSubscriptions.size(); i++) {
                                final JsonArray subscription = topicSubscriptions.getJsonArray(i);
                                if (isSubscriptionAvailable(subscription)) {
                                    if (isEmpty(subscription)) {
                                        return;
                                    }
                                }
                            }
                        }
                );
    }

    private static boolean isSubscriptionAvailable(final JsonArray subscription) {
        return subscription.get(SUBSCRIPTION_NAME_INDEX).toString().startsWith(SUBSCRIPTION_NAME);

    }

    private static boolean isEmpty(final JsonArray subscription) {
        return (((JsonNumber) subscription.get(SUBSCRIPTION_MESSAGE_COUNT_INDEX)).intValue() == EMPTY_SUBSCRIPTION_COUNT) ? true : false;
    }
}
