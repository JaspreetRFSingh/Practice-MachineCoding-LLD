# PubSubSystem — Low Level Design Notes

## Problem Summary

An in-memory single-queue publish/subscribe system:
- One global FIFO queue; messages are append-only
- Subscribers register a filter of eventTypes; non-matching messages are silently ignored
- A subscriber only receives messages published **while it is active** (no retroactive delivery)
- Subscribers may be removed and re-added; processed-message counts accumulate across sessions

---

## Class Diagram

```
<<interface>> Subject
  + subscribe(Observer)
  + unsubscribe(subscriberId)
  + notifyObservers(QueueMessage)
        ▲
        │ implements
  QueueManager
    ├── List<QueueMessage> queue                  ← global FIFO, append-only
    ├── LinkedHashMap<subscriberId, Observer>     ← active subscribers (insertion order)
    ├── HashMap<subscriberId, int> historicalCounts
    └── int nextSequenceId

<<interface>> Observer
  + onMessage(QueueMessage)
  + getSubscriberId()
  + getSessionProcessedCount()
        ▲
        │ implements
  Subscriber
    ├── subscriberId
    ├── Set<String> eventTypes   ← lowercase, O(1) lookup
    └── int sessionProcessedCount

QueueMessage (immutable value object)
  ├── int sequenceId
  ├── String eventType           ← lowercase
  └── String payload

Solution  (thin facade)
  └── QueueManager               ← single composition root
```

---

## Design Patterns

### Observer Pattern

**Why:** The problem explicitly mandates it. QueueManager is the Subject; each Subscriber is an Observer. The Subject knows nothing about how each Observer filters — it simply broadcasts and the Observer self-filters. This keeps QueueManager open/closed: adding a new subscriber type (e.g. a logger, a metrics collector) requires zero changes to QueueManager.

```
QueueManager (Subject)            Subscriber (Observer)
─────────────────────             ─────────────────────
subscribe(observer)     ──────►   stored in activeSubscribers
publish(eventType, msg)
  → append to queue
  → notifyObservers(msg) ──────►  onMessage(msg)
                                    if eventType in filter:
                                      sessionProcessedCount++
```

---

## Key Algorithms

### subscribe (addSubscriber)

```
If same subscriberId is already active:
  flushAndRemove(id)   ← saves current session count into historicalCounts
Create new Subscriber(id, eventTypesToProcess)
activeSubscribers.put(id, subscriber)
```

This handles both fresh subscribe and re-subscribe-without-prior-remove in one path.

### unsubscribe (removeSubscriber)

```
If id is active:
  observer = activeSubscribers.remove(id)
  historicalCounts.merge(id, observer.sessionProcessedCount, Integer::sum)
```

The flush ensures past counts survive future re-subscriptions.

### publish (sendMessage)

```
message = new QueueMessage(nextSequenceId++, eventType, payload)
queue.add(message)                      ← FIFO append
for each observer in activeSubscribers: ← only currently active
    observer.onMessage(message)
```

Late joiners never see pre-subscription messages because they are not in `activeSubscribers` at notification time.

### countProcessedMessages

```
historical = historicalCounts.getOrDefault(id, 0)
current    = activeSubscribers.get(id)?.sessionProcessedCount ?: 0
return historical + current
```

O(1). Works whether the subscriber is currently active or not.

---

## Cross-Session Count Accounting

```
Timeline for S1:

addSubscriber("S1", ["ORDER","PAYMENT"])
  sessionCount = 0,  historical = 0

[o-1 arrives] → S1 processes ORDER → sessionCount = 1
[p-1 arrives] → S1 processes PAYMENT → sessionCount = 2

removeSubscriber("S1")
  flush: historical["S1"] = 0 + 2 = 2,  S1 removed

[o-2 arrives] → S1 not active, nobody notified

addSubscriber("S1", ["ORDER","REFUND"])
  new sessionCount = 0,  historical["S1"] still = 2

[p-2 already happened before re-subscribe — not retroactive]
[r-1 arrives] → S1 processes REFUND → sessionCount = 1

countProcessedMessages("S1") = historical(2) + session(1) = 3
```

Wait — in the example S1 also processes p-2 before being removed. Let me re-trace:

```
addSubscriber S1 ["ORDER","PAYMENT"]
  session=0, hist=0

o-1 → ORDER ✓  session=1
p-1 → PAYMENT ✓ session=2
p-2 → PAYMENT ✓ session=3

removeSubscriber S1
  flush: hist["S1"] = 3

addSubscriber S1 ["ORDER","REFUND"]
  new session=0, hist=3

r-1 → REFUND ✓ session=1

count = hist(3) + session(1) = 4 ✓
```

---

## Key Data-Flow

### addSubscriber
```
Solution.addSubscriber(id, eventTypes)
  → new Subscriber(id, eventTypes)
  → QueueManager.subscribe(subscriber)
       → if already active: flushAndRemove(id)
       → activeSubscribers.put(id, subscriber)
```

### removeSubscriber
```
Solution.removeSubscriber(id)
  → QueueManager.unsubscribe(id)
       → flushAndRemove(id)
            → historicalCounts.merge(id, sessionCount, Integer::sum)
            → activeSubscribers.remove(id)
```

### sendMessage
```
Solution.sendMessage(eventType, payload)
  → QueueManager.publish(eventType, payload)
       → queue.add(new QueueMessage(...))
       → for each activeSubscriber:
            observer.onMessage(message)
              → if eventType in filter: sessionProcessedCount++
```

---

## Design Decisions

| Decision | Rationale |
|---|---|
| Observer pattern (Subject/Observer interfaces) | Required by spec; also gives clean extensibility — new observer types don't touch QueueManager |
| `historicalCounts` flushed on unsubscribe | Decouples lifetime totals from per-session state; Subscriber stays small and stateless |
| `LinkedHashMap` for activeSubscribers | Deterministic notification order (first subscribed = first notified) without extra sorting |
| `subscribe()` calls `flushAndRemove` if already active | Single code path for first-time subscribe and re-subscribe; avoids silent count loss |
| eventType lowercased at ingestion | Case-insensitivity handled once; all lookups are O(1) HashSet compares |
| Queue is append-only `ArrayList` | FIFO order is insertion order; no removal needed since late subscribers use notification, not replay |
| Solution is a thin facade | All logic in QueueManager; Solution is easily replaced with a REST controller or gRPC handler |

---

## Constraints & Assumptions

- Single-threaded environment (no concurrency handling)
- No retroactive delivery — a subscriber only sees messages sent after it subscribes
- `countProcessedMessages` accumulates across all sessions for the same subscriberId
- eventType matching is case-insensitive
- `removeSubscriber` on a non-existent id is a no-op
