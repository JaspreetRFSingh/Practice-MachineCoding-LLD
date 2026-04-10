package PubSubSystem;

/**
 * Observer role in the Observer pattern.
 * Each Subscriber implements this interface and is notified via onMessage()
 * whenever the Subject (QueueManager) appends a new message.
 */
public interface Observer {
    /** Called by the Subject when a new message arrives in the queue. */
    void onMessage(QueueMessage message);

    String getSubscriberId();

    /** Messages actually processed (eventType matched) in the current session. */
    int getSessionProcessedCount();
}
