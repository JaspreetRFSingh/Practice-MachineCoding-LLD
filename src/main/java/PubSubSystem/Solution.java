package PubSubSystem;

import java.util.List;

/**
 * Public API facade for the Single-Queue Pub/Sub system.
 *
 * Design patterns:
 *   Observer — QueueManager (Subject) notifies Subscribers (Observers)
 *              synchronously on every sendMessage call.
 *
 * All business logic lives in QueueManager; Solution is a thin delegation layer.
 */
public class Solution {

    private final QueueManager queueManager = new QueueManager();

    /**
     * Registers a subscriber. If the same subscriberId is already active,
     * the existing subscription is replaced (old session count preserved,
     * eventTypesToProcess replaced). If it was previously removed and is
     * being re-added, past processed counts are accumulated.
     */
    public void addSubscriber(String subscriberId, List<String> eventTypesToProcess) {
        Subscriber subscriber = new Subscriber(subscriberId, eventTypesToProcess);
        queueManager.subscribe(subscriber);   // QueueManager handles replace/flush internally
    }

    /**
     * Unsubscribes the given subscriber immediately.
     * No-op if the subscriber is not currently active.
     * The subscriber may be re-added later via addSubscriber.
     */
    public void removeSubscriber(String subscriberId) {
        queueManager.unsubscribe(subscriberId);
    }

    /**
     * Appends a message to the global FIFO queue and synchronously notifies
     * all currently active subscribers. Subscribers with non-matching eventType
     * silently ignore the message.
     */
    public void sendMessage(String eventType, String message) {
        queueManager.publish(eventType, message);
    }

    /**
     * Returns the total number of messages actually processed (eventType matched)
     * by the given subscriber across all of their subscribe sessions.
     */
    public int countProcessedMessages(String subscriberId) {
        return queueManager.getTotalProcessedCount(subscriberId);
    }
}
