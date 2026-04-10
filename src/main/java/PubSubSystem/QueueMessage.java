package PubSubSystem;

/**
 * Immutable value object representing one message in the global FIFO queue.
 * eventType is stored lowercase so all comparisons are case-insensitive.
 */
public class QueueMessage {
    final int sequenceId;
    final String eventType;  // stored lowercase
    final String payload;

    public QueueMessage(int sequenceId, String eventType, String payload) {
        this.sequenceId = sequenceId;
        this.eventType  = eventType.toLowerCase();
        this.payload    = payload;
    }
}
