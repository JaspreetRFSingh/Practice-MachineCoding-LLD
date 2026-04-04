package RateLimiter;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sliding Window Counter (log-based) strategy.
 *
 * For each incoming request at time `t`, the active window is [t - timePeriod + 1, t].
 * All timestamps outside that range are evicted from the front of the deque.
 * The request is allowed if the remaining count < maxRequests.
 *
 * This eliminates the boundary-burst problem of fixed windows at the cost of
 * storing up to `maxRequests` timestamps in memory per resource.
 *
 * Time  : O(k) per isAllowed in the worst case, where k = evicted timestamps.
 *         Amortised O(1) because each timestamp is inserted and removed at most once.
 * Space : O(maxRequests) — deque never grows beyond the allowed limit
 */
public class SlidingWindowCounterStrategy implements RateLimitStrategy {

    private final int maxRequests;
    private final int timePeriod;
    // Deque of allowed-request timestamps, ordered oldest → newest
    private final Deque<Integer> window = new ArrayDeque<>();

    public SlidingWindowCounterStrategy(int maxRequests, int timePeriod) {
        this.maxRequests = maxRequests;
        this.timePeriod = timePeriod;
    }

    @Override
    public boolean isAllowed(int timestamp) {
        int windowStart = timestamp - timePeriod + 1;
        // Evict timestamps that have fallen outside the current window
        while (!window.isEmpty() && window.peekFirst() < windowStart) {
            window.pollFirst();
        }
        if (window.size() < maxRequests) {
            window.addLast(timestamp);
            return true;
        }
        return false;
    }
}
