# JobScheduler — Low Level Design Notes

## Problem Summary

A distributed job scheduler that assigns incoming jobs to machines:
- Each machine has a set of capabilities
- Each job requires a set of capabilities — only a compatible machine (superset) may run it
- Multiple selection algorithms (criteria) decide which compatible machine is chosen
- Tiebreaker: lexicographically smallest machineId

---

## Class Diagram

```
Solution
  ├── Map<machineId, Machine>
  ├── Map<jobId, machineId>              ← for O(1) jobCompleted lookup
  └── Map<criteria int, AssignmentCriteria>   ← strategy registry

Machine
  ├── machineId
  ├── Set<String> capabilities           ← stored lowercase
  ├── int unfinishedCount
  └── int finishedCount

<<interface>>
AssignmentCriteria
  └── Comparator<Machine> getComparator()

LeastUnfinishedCriteria   ← criteria 0
MostFinishedCriteria      ← criteria 1
```

---

## Design Patterns

### Strategy Pattern

**Why:** The problem explicitly requires extensibility for new selection algorithms.
Each criteria is an independent strategy — adding a new one requires zero changes
to existing code.

```
<<interface>>
AssignmentCriteria
+ getComparator() : Comparator<Machine>
        ▲                ▲
        │                │
LeastUnfinished     MostFinished
Criteria            Criteria
(criteria = 0)      (criteria = 1)
```

To add a new criteria (e.g. criteria 2: fewest total jobs ever assigned):
1. Create a class implementing `AssignmentCriteria`
2. Register it in `Solution`'s constructor: `criteriaRegistry.put(2, new MyNewCriteria())`

---

## Key Algorithms

### Machine selection (`assignMachineToJob`)

```
1. Filter: machines where capabilities ⊇ capabilitiesRequired   O(m × k)
2. If no candidates → return ""
3. Sort candidates:
     primary   = criteria comparator (e.g. ascending unfinishedCount)
     secondary = ascending machineId (lexicographic tiebreaker)
4. Assign job to candidates[0], increment its unfinishedCount
5. Store jobId → machineId in jobToMachine map
```

- m = number of machines (≤ 100), k = required capabilities (≤ 100)
- Sorting is O(m log m) — negligible at these constraints

### Capability check (`hasCapabilities`)

```
for each required cap (lowercased):
    if not in machine's HashSet → return false
return true
```

O(k) per machine backed by a `HashSet<String>`.

### Job completion (`jobCompleted`)

```
machineId = jobToMachine.get(jobId)   ← O(1)
machine.unfinishedCount--
machine.finishedCount++
```

---

## Key Data-Flow

### addMachine
```
Solution.addMachine(machineId, capabilities[])
  → new Machine(machineId, capabilities)   ← lowercases all caps into a HashSet
  → stored in machines map
```

### assignMachineToJob
```
Solution.assignMachineToJob(jobId, capabilitiesRequired[], criteria)
  → look up AssignmentCriteria from registry
  → filter compatible machines
  → sort by criteria comparator + machineId tiebreaker
  → selected = candidates[0]
  → selected.assignJob()      ← unfinishedCount++
  → jobToMachine.put(jobId, machineId)
  → return machineId
```

### jobCompleted
```
Solution.jobCompleted(jobId)
  → O(1) lookup: jobToMachine.get(jobId)
  → machine.completeJob()    ← unfinishedCount--, finishedCount++
```

---

## Design Decisions

| Decision | Rationale |
|---|---|
| Strategy pattern via `AssignmentCriteria` interface | Extensibility requirement; new criteria = new class, no edits to `Solution` |
| Tiebreaker applied via `.thenComparing(Machine::getMachineId)` | Composable, no special-case logic needed |
| Capabilities stored lowercase at ingestion | Case-insensitivity handled once; all comparisons are O(1) HashSet lookups |
| `jobToMachine` map for completion | O(1) lookup on `jobCompleted`; avoids scanning all machines |
| Criteria registry as `Map<Integer, AssignmentCriteria>` | Clean open/closed principle — register new criteria without modifying selection logic |

---

## Constraints & Assumptions

- Single-threaded environment — no concurrency handling needed
- At most 100 machines, 100 distinct capabilities
- `machineId` and `jobId` are always unique and non-blank
- `jobCompleted` is only called for previously assigned jobs (no validation needed)
- Capability tokens are case-insensitive; all stored and compared in lowercase
- No partial assignment — a job is either fully assigned to one machine or not at all
