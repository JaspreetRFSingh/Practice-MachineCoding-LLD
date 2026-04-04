package RateLimiter;

/**
 * Fixed Window Counter strategy.
 *
 * Divides the time axis into non-overlapping windows of size `timePeriod`.
 * Window for a given timestamp = [floor(t / timePeriod) * timePeriod, next boundary).
 * Tracks a single counter per window; resets when the window changes.
 *
 * Trade-off: a burst of `maxRequests` requests can occur at the very end of one
 * window and another `maxRequests` at the very start of the next — effectively
 * 2× the limit in a span of 1 second. Use SlidingWindowCounterStrategy when
 * this burst behavior is unacceptable.
 *
 * Time  : O(1) per isAllowed
 * Space : O(1) — only the current window start and count are stored
 */
public class FixedWindowCounterStrategy implements RateLimitStrategy {

    private final int maxRequests;
    private final int timePeriod;
    private int windowStart = -1;
    private int count = 0;

    public FixedWindowCounterStrategy(int maxRequests, int timePeriod) {
        this.maxRequests = maxRequests;
        this.timePeriod = timePeriod;
    }

    @Override
    public boolean isAllowed(int timestamp) {
        int currentWindow = (timestamp / timePeriod) * timePeriod;
        if (currentWindow != windowStart) {
            windowStart = currentWindow;
            count = 0;
        }
        if (count < maxRequests) {
            count++;
            return true;
        }
        return false;
    }
}
