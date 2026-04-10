package PubSubSystem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete Observer.
 * Holds a session-scoped processed count; the QueueManager accumulates
 * cross-session totals in historicalCounts so this class stays stateless
 * with respect to lifetime totals.
 */
public class Subscriber implements Observer {

    private final String subscriberId;
    private final Set<String> eventTypes;  // stored lowercase
    private int sessionProcessedCount;

    public Subscriber(String subscriberId, List<String> eventTypesToProcess) {
        this.subscriberId = subscriberId;
        this.eventTypes = new HashSet<>();
        for (String et : eventTypesToProcess) {
            this.eventTypes.add(et.toLowerCase());
        }
        this.sessionProcessedCount = 0;
    }

    /**
     * Called by QueueManager.notifyObservers on every new message.
     * Increments count only when eventType is in this subscriber's filter.
     */
    @Override
    public void onMessage(QueueMessage message) {
        if (eventTypes.contains(message.eventType)) {
            sessionProcessedCount++;
        }
    }

    @Override
    public String getSubscriberId() {
        return subscriberId;
    }

    @Override
    public int getSessionProcessedCount() {
        return sessionProcessedCount;
    }
}
