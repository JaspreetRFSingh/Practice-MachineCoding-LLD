package PubSubSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete Subject (QueueManager) in the Observer pattern.
 *
 * Responsibilities:
 *   - Owns the single global FIFO queue of QueueMessages.
 *   - Maintains the set of currently active Observers (activeSubscribers).
 *   - Notifies every active Observer synchronously on each publish.
 *   - Accumulates processed-message counts across subscribe/unsubscribe cycles
 *     in historicalCounts (keyed by subscriberId).
 *
 * Cross-session count accounting:
 *   On unsubscribe  → sessionCount is flushed into historicalCounts.
 *   On re-subscribe → a fresh Subscriber starts at 0; historicalCounts is unchanged.
 *   On query        → historicalCounts + (active session count if currently subscribed).
 */
public class QueueManager implements Subject {

    // Global FIFO queue — append-only
    private final List<QueueMessage> queue = new ArrayList<>();
    private int nextSequenceId = 0;

    // Insertion-ordered map so notification order is deterministic (first subscribed = first notified)
    private final Map<String, Observer> activeSubscribers = new LinkedHashMap<>();

    // Cumulative counts for all completed sessions (including removed subscribers)
    private final Map<String, Integer> historicalCounts = new HashMap<>();

    // -------------------------------------------------------------------------
    // Subject interface
    // -------------------------------------------------------------------------

    /**
     * Registers an Observer.
     * If a subscriber with the same ID is already active, it is first unsubscribed
     * (flushing its current session count) before the new one is installed.
     * This covers the re-subscribe-without-explicit-remove case.
     */
    @Override
    public void subscribe(Observer observer) {
        String id = observer.getSubscriberId();
        if (activeSubscribers.containsKey(id)) {
            flushAndRemove(id);  // preserve current session count before replacing
        }
        activeSubscribers.put(id, observer);
    }

    /**
     * Deregisters an Observer by ID and flushes its session count.
     * No-op if the subscriber is not currently active.
     */
    @Override
    public void unsubscribe(String subscriberId) {
        if (activeSubscribers.containsKey(subscriberId)) {
            flushAndRemove(subscriberId);
        }
    }

    /**
     * Push a new message to all currently active Observers.
     * Each Observer independently decides whether to count it (based on its own filter).
     */
    @Override
    public void notifyObservers(QueueMessage message) {
        for (Observer observer : activeSubscribers.values()) {
            observer.onMessage(message);
        }
    }

    // -------------------------------------------------------------------------
    // Publishing
    // -------------------------------------------------------------------------

    public void publish(String eventType, String payload) {
        QueueMessage message = new QueueMessage(nextSequenceId++, eventType, payload);
        queue.add(message);
        notifyObservers(message);
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    public int getTotalProcessedCount(String subscriberId) {
        int historical = historicalCounts.getOrDefault(subscriberId, 0);
        Observer active = activeSubscribers.get(subscriberId);
        int currentSession = (active != null) ? active.getSessionProcessedCount() : 0;
        return historical + currentSession;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void flushAndRemove(String subscriberId) {
        Observer observer = activeSubscribers.remove(subscriberId);
        historicalCounts.merge(subscriberId, observer.getSessionProcessedCount(), Integer::sum);
    }
}
