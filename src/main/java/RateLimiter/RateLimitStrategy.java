package RateLimiter;

/**
 * Strategy interface for rate limiting algorithms.
 * Implement this to add a new rate limiting strategy.
 * Each instance is scoped to a single resource, so implementations
 * may store per-resource state (counters, timestamps) directly as fields.
 */
public interface RateLimitStrategy {
    boolean isAllowed(int timestamp);
}
