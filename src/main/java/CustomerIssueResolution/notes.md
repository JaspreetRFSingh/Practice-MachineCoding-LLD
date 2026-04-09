# CustomerIssueResolution — Low Level Design Notes

## Problem Summary

A multi-threaded customer support system for an e-commerce platform:
- Issues are created for orders with a specific type (e.g. "order delayed", "damaged product")
- Agents have expertise in a subset of issue types
- Issues are assigned to agents using one of three strategies
- Resolved issues are tracked per agent

---

## Class Diagram

```
Solution
  ├── List<String> issueTypes                        ← lowercased at ingestion
  ├── ConcurrentHashMap<issueId, Issue>
  ├── ConcurrentHashMap<agentId, Agent>
  ├── ReentrantLock assignLock                       ← guards assign + resolve
  └── Map<strategyInt, AssignmentStrategy>           ← strategy registry

Issue
  ├── issueId, orderId, issueType, description
  ├── IssueStatus status                             ← OPEN → ASSIGNED → RESOLVED
  └── assignedAgentId                                ← null until assigned

Agent
  ├── agentId
  ├── Set<Integer> expertise
  ├── int totalOpenIssues                            ← guarded by assignLock
  ├── Map<issueType, int> openByType                 ← guarded by assignLock
  ├── Map<issueType, int> resolvedByType             ← guarded by assignLock
  └── CopyOnWriteArrayList<String> resolvedHistory   ← written under lock, read lock-free

<<interface>>
AssignmentStrategy
  └── Comparator<Agent> getComparator(int issueType)

LeastTotalOpenStrategy     ← strategy 0
MostResolvedByTypeStrategy ← strategy 1
LeastOpenByTypeStrategy    ← strategy 2
```

---

## Design Patterns

### Strategy Pattern

**Why:** Three distinct, independently swappable selection algorithms are required.
Each strategy is a single class; adding a fourth strategy requires zero edits to `Solution`.

```
<<interface>>
AssignmentStrategy
+ getComparator(issueType) : Comparator<Agent>
        ▲               ▲               ▲
        │               │               │
LeastTotalOpen  MostResolvedByType  LeastOpenByType
(strategy 0)      (strategy 1)       (strategy 2)
```

To add strategy 3 (e.g. round-robin):
1. Implement `AssignmentStrategy`
2. Register it: `strategyRegistry.put(3, new RoundRobinStrategy())`

---

## Thread Safety Design

### The concurrency problem

`assignIssue` must **read** agent counters (totalOpen, openByType, resolvedByType) and then
**write** them atomically. `resolveIssue` writes those same counters.
If they ran concurrently, a resolve mid-selection could silently skew the chosen agent.

### Solution: one ReentrantLock covers both

```
assignLock guards:
  ├── assignIssue  (read counters → pick agent → write counters + issue.status)
  └── resolveIssue (write counters + issue.status)
```

**Why not finer-grained per-agent locks?**
To compare *multiple* agents, we'd need to lock all of them simultaneously — that's a classic
multi-lock deadlock risk requiring a global ordering. A single mutex is simpler, avoids deadlocks,
and is justified because assignment/resolution are CPU-bound (no I/O inside the critical section).

### ConcurrentHashMap for issue/agent maps

- `issues.putIfAbsent` makes `createIssue` duplicate-check atomic without the assignLock
- `agents.putIfAbsent` does the same for `addAgent`

### CopyOnWriteArrayList for resolvedHistory

- Writes happen under `assignLock` (inside `resolveIssue`)
- `getAgentHistory` reads without holding any lock — COW provides a safe snapshot
- Preferred over `Collections.synchronizedList` because reads far outnumber writes

---

## Key Algorithms

### assignIssue

```
1. Validate issue exists                               O(1)
2. Acquire assignLock
3. Check issue.status == OPEN (prevents double-assign) O(1)
4. Filter agents by expertise.contains(issueType)      O(a)
5. Sort eligible agents by strategy comparator         O(e log e)
6. selected = eligible[0]
7. selected.onAssigned(issueType)   ← increment counters
8. issue.status = ASSIGNED, issue.assignedAgentId = selected
9. Return agentId
```
- a = total agents (≤ 1000), e = eligible agents (≤ a)

### resolveIssue

```
1. Validate issue exists                               O(1)
2. Acquire assignLock
3. Check issue.status == ASSIGNED
4. agent.onResolved(issueId, issueType)
     └─ totalOpenIssues--, openByType--, resolvedByType++, resolvedHistory.add
5. issue.status = RESOLVED
6. Release lock, then print log (outside lock to reduce hold time)
```

### getAgentHistory

```
1. Lookup agent in ConcurrentHashMap                   O(1)
2. Return new ArrayList snapshot of CopyOnWriteArrayList
   (no lock needed — COW iterator is point-in-time consistent)
```

---

## Key Data-Flow

### createIssue
```
Solution.createIssue(issueId, orderId, issueType, description)
  → validate issueType index in range
  → issues.putIfAbsent(issueId, new Issue(...))   ← atomic, no lock needed
  → return "issue created" or "issue already exists"
```

### addAgent
```
Solution.addAgent(agentId, expertise)
  → agents.putIfAbsent(agentId, new Agent(agentId, expertiseSet))
  → return "success" or "agent already exists"
```

### assignIssue
```
Solution.assignIssue(issueId, strategy)
  → issues.get(issueId)
  → assignLock.lock()
  → filter + sort eligible agents via strategy comparator
  → selected.onAssigned(issueType)
  → issue.status = ASSIGNED
  → assignLock.unlock()
  → return agentId
```

### resolveIssue
```
Solution.resolveIssue(issueId, resolution)
  → issues.get(issueId)
  → assignLock.lock()
  → agent.onResolved(issueId, issueType)
  → issue.status = RESOLVED
  → assignLock.unlock()
  → helper.print(...)    ← outside lock; log is not part of critical state
```

---

## Design Decisions

| Decision | Rationale |
|---|---|
| Strategy pattern for all three assign modes | Open/closed principle — new strategies don't modify `Solution` |
| Single `assignLock` for assign + resolve | Prevents TOCTOU race on agent counters; no deadlock risk; fast critical section |
| `ConcurrentHashMap.putIfAbsent` for create/add | Lock-free duplicate detection for independent creation paths |
| `CopyOnWriteArrayList` for resolvedHistory | `getAgentHistory` reads vastly outnumber writes; avoids locking on every read |
| Issue type stored as `int` index, not `String` | O(1) lookup for expertise matching and all counter maps |
| Strategy comparator + stable sort | Ties naturally resolve to insertion order — satisfies "any one of them" deterministically |
| Log printed outside `assignLock` | Lock hold time is minimised; logging has no effect on system correctness |
| `issueTypes` lowercased at `init` | Case-insensitivity handled once at ingestion, not on every comparison |

---

## Constraints & Assumptions

- Up to 20 issue types, 1000 agents, 10^5 issues
- `issueId`, `orderId`, `agentId` are unique and non-empty
- `resolveIssue` is only called for existing, assigned issues
- `getAgentHistory` returns resolved issues in chronological resolution order
- Multiple threads may call any method concurrently
