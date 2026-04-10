package PubSubSystem;

/**
 * Subject role in the Observer pattern.
 * QueueManager implements this interface and maintains the list of active Observers.
 */
public interface Subject {
    void subscribe(Observer observer);
    void unsubscribe(String subscriberId);
    void notifyObservers(QueueMessage message);
}
