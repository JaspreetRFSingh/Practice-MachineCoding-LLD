# RateLimiter — Low Level Design Notes

## Problem Summary

An in-memory rate limiter that:
- Tracks request counts per resource (`resourceId`)
- Supports multiple pluggable algorithms (strategies) per resource
- Allows live reconfiguration of strategy and limits for a resource

---

## Class Diagram

```
RateLimiter
  ├── Map<resourceId, RateLimitStrategy>          ← live strategy per resource
  └── Map<strategyName, BiFunction<int,int,RateLimitStrategy>>  ← factory registry

<<interface>>
RateLimitStrategy
  └── boolean isAllowed(int timestamp)
          ▲                   ▲
          │                   │
FixedWindowCounter     SlidingWindowCounter
Strategy               Strategy
```

---

## Design Patterns

### Strategy Pattern

**Why:** Each resource can independently use a different rate limiting algorithm, and new algorithms must be addable without modifying existing code (Open/Closed Principle). The interface is intentionally minimal — `isAllowed(timestamp)` — so any algorithm fits without exposing its internals.

**How extensibility works:**
1. Implement `RateLimitStrategy`
2. Register in `RateLimiter`'s constructor:
   ```java
   strategyRegistry.put("token-bucket", TokenBucketStrategy::new);
   ```
   Zero changes to any existing class.

### Factory via `BiFunction` Registry

Each entry in `strategyRegistry` is a `BiFunction<Integer, Integer, RateLimitStrategy>` — a constructor reference that takes `(maxRequests, timePeriod)` and produces a fresh strategy instance. This avoids a separate factory class for each strategy while keeping construction logic encapsulated.

---

## Algorithm Details

### Fixed Window Counter

```
window_start = floor(timestamp / timePeriod) * timePeriod

if timestamp falls in a new window:
    reset count = 0, update window_start

if count < maxRequests:
    count++; return true
else:
    return false
```

**Known weakness — boundary burst:** A caller can make `maxRequests` calls just before a window boundary and another `maxRequests` calls just after, yielding `2 × maxRequests` in `timePeriod` seconds. Acceptable when simplicity and O(1) space matter more than strict smoothing.

**Time:** O(1) | **Space:** O(1) — only `windowStart` and `count` stored.

---

### Sliding Window Counter (log-based)

```
windowStart = timestamp - timePeriod + 1

evict all timestamps < windowStart from front of deque

if deque.size() < maxRequests:
    deque.addLast(timestamp); return true
else:
    return false
```

The deque is ordered oldest → newest. Because timestamps are always non-decreasing (problem guarantee), insertion is always at the tail and eviction is always from the head — a `Deque` (not a `TreeMap` or sorted list) suffices.

**Why the window is [t - timePeriod + 1, t] (inclusive on both ends):**
A `timePeriod` of 3 means "any 3-second span". At t=8 with timePeriod=3, the relevant span is [6, 8] — 3 seconds. Using `t - timePeriod` would give [5, 8] = 4 seconds, which is wrong.

**Time:** O(k) worst case per call where k = evicted timestamps; **amortised O(1)** because each timestamp enters and leaves the deque exactly once.
**Space:** O(maxRequests) — the deque never exceeds the limit.

---

## Reconfiguration Behaviour

When `addResource` is called again for an existing `resourceId`, the old strategy instance is replaced entirely. The new strategy starts with a clean slate — no carry-over of state from the previous configuration. This is the safest default: partial state from a different window/algorithm would produce undefined results.

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| `RateLimitStrategy` instance is per-resource | Each resource needs isolated state (counter/log). A single shared instance would conflate counts across resources. |
| Factory stored as `BiFunction` reference | Constructor references (`FixedWindowCounterStrategy::new`) act as factories without a separate Factory class — less ceremony, same extensibility. |
| `Deque<Integer>` for sliding window log | Timestamps are monotonically non-decreasing (problem guarantee), so a deque with O(1) head-eviction and tail-insertion is optimal. A `TreeSet` or `PriorityQueue` would be O(log n) unnecessarily. |
| Reconfiguration replaces the instance, not the fields | Avoids leaking state across configurations with different algorithms or parameters. Clean break is simpler and safer. |
| Strategy interface has no `configure()` method | Parameters are injected at construction time (via the factory). Keeping `isAllowed` as the sole method prevents callers from mutating live configuration, and makes each strategy immutable with respect to its parameters. |

---

## Complexity Summary

| Strategy | isAllowed Time | isAllowed Space |
|---|---|---|
| Fixed Window Counter | O(1) | O(1) |
| Sliding Window Counter | O(k) worst / O(1) amortised | O(maxRequests) |

---

## Constraints & Assumptions

- Single-threaded — no locking needed
- Timestamps across all resources are **globally non-decreasing** (problem guarantee)
- `isAllowed` for a blocked request does **not** record the timestamp — only allowed requests are logged in the sliding window
- `timePeriod` is in seconds; timestamp unit is seconds (1 time unit = 1 second)
- `addResource` with an existing `resourceId` is a full reconfiguration, not a partial update
