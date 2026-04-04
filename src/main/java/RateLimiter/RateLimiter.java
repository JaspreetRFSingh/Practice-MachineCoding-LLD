package RateLimiter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class RateLimiter {

    // Live strategy instance per resource (replaced entirely on reconfiguration)
    private final Map<String, RateLimitStrategy> resources = new HashMap<>();

    // Registry: strategy name → factory(maxRequests, timePeriod) → strategy instance
    // Open/Closed: add a new strategy by registering it here; no other code changes needed.
    private final Map<String, BiFunction<Integer, Integer, RateLimitStrategy>> strategyRegistry = new HashMap<>();

    public RateLimiter() {
        strategyRegistry.put("fixed-window-counter",  FixedWindowCounterStrategy::new);
        strategyRegistry.put("sliding-window-counter", SlidingWindowCounterStrategy::new);
    }

    /**
     * Registers (or reconfigures) a resource.
     * Reconfiguration discards all prior state — the new strategy starts fresh.
     */
    public void addResource(String resourceId, String strategy, String limits) {
        String[] parts = limits.split(",");
        int maxRequests = Integer.parseInt(parts[0].trim());
        int timePeriod  = Integer.parseInt(parts[1].trim());

        BiFunction<Integer, Integer, RateLimitStrategy> factory = strategyRegistry.get(strategy);
        if (factory == null) throw new IllegalArgumentException("Unknown strategy: " + strategy);

        resources.put(resourceId, factory.apply(maxRequests, timePeriod));
    }

    public boolean isAllowed(String resourceId, int timestamp) {
        return resources.get(resourceId).isAllowed(timestamp);
    }
}
