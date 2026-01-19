# Meeting Room Reservation System

## Problem Statement

Design a meeting room reservation system for a fixed list of conference rooms. Support booking and canceling meetings while ensuring no two meetings overlap in the same room.

## Scheduling Rules

- Time ranges are **inclusive**: a meeting ending at `t` conflicts with another starting at `t` in the same room.
- If multiple rooms are free for a requested time, always choose the **lexicographically smallest** room id.
- At most one meeting can occur in a room at any moment.
- `meetingId` values are strings; an id cannot refer to more than one active meeting at a time.

## API

```java
RoomBooking(List<String> roomIds)
String  bookMeeting(String meetingId, int startTime, int endTime)
boolean cancelMeeting(String meetingId)
```

### Examples

```java
RoomBooking rb = new RoomBooking(Arrays.asList("roomA", "roomB"));
rb.bookMeeting("m1", 10, 20);   // "roomA"
rb.bookMeeting("m2", 15, 25);   // "roomB"
rb.bookMeeting("m3", 20, 30);   // "" — 20 is inclusive end, conflicts with both rooms
rb.cancelMeeting("m1");         // true
rb.bookMeeting("m4", 20, 30);   // "roomA" — freed by cancel

RoomBooking rb2 = new RoomBooking(Arrays.asList("Z1", "A1", "M3"));
rb2.bookMeeting("x", 5, 5);    // "A1" — lex-smallest
rb2.bookMeeting("y", 5, 6);    // "M3" — A1 has [5,5] which conflicts at 5
rb2.cancelMeeting("nope");     // false
rb2.bookMeeting("z", 6, 10);   // "A1" — [5,5] and [6,10] do not overlap
```

## Constraints

- `1 ≤ rooms ≤ 50,000`, `0 ≤ startTime ≤ endTime ≤ 10^9`
- `Total operations ≤ 100,000`
- `meetingId`, room ids: non-empty strings, length ≤ 50

---

## Why a Naïve Linear Scan Fails at Scale

The naïve approach: iterate all R rooms in lex order, lock each one, call `isRoomFree`. That is O(R · log M) per booking.

Two problems with R = 50,000:

1. **Wasted work** — most rooms have zero meetings at any moment. A room with no meetings is trivially available for *any* interval. Yet the naïve scan locks it and runs the check.
2. **Lock churn** — acquiring and releasing 50,000 per-room locks per booking saturates the OS thread scheduler even when each lock is uncontested.

---

## Core Insight

> A room with **zero** active meetings is available for every possible `[start, end]`.
> Therefore, the answer to any booking query is either:
> - an occupied room (≥1 meeting but no conflict for *this* interval) that is lex-smaller than the first idle room, OR
> - the first idle room itself.
>
> No room lex-*larger* than the first idle room can ever win — the idle room is always available and smaller.

This means: **only scan occupied rooms that come before the first idle room**.

---

## Data Structures

| Structure | Type | Role |
|-----------|------|------|
| `roomIds` | `List<String>` (sorted, immutable) | Lex-ordered room list for Phase-3 fallback |
| `roomIndexMap` | `HashMap<String, Integer>` | roomId → array index; O(1) lookup |
| `roomSchedules[i]` | `ConcurrentSkipListMap<Integer, Integer>` | Per-room schedule: startTime → endTime; O(log M) floor/ceiling overlap queries |
| `roomLocks[i]` | `ReentrantLock` | Per-room lock for atomic check-and-book |
| `freeRooms` | `ConcurrentSkipListSet<String>` | Rooms with **zero** meetings; always available for any interval |
| `occupiedRooms` | `ConcurrentSkipListSet<String>` | Rooms with **≥1** meeting; complement of `freeRooms` |
| `activeMeetings` | `ConcurrentHashMap<String, MeetingInfo>` | meetingId → (room, start, end); O(1) cancel lookup |

`freeRooms` and `occupiedRooms` are strict complements — every room is in exactly one at all times (maintained under per-room locks).

### Why ConcurrentSkipListMap for the room schedule?

`ConcurrentSkipListMap` is a thread-safe sorted map. `floorEntry(start)` and `ceilingEntry(start)` are O(log M) and can be called without the room lock (for reading), though we always call them while holding the lock here. Its sorted nature makes the two-sided overlap check natural and efficient.

### Why ConcurrentSkipListSet for freeRooms / occupiedRooms?

Both sets are read (via `first()` and `headSet()`) *outside* locks to take a snapshot, then mutated *inside* per-room locks. `ConcurrentSkipListSet` provides:
- `first()` in O(log R) — lex-smallest idle room
- `headSet(bound, exclusive)` as a live view — only occupied rooms before the bound
- Weakly-consistent iteration — no `ConcurrentModificationException` during concurrent mutation

---

## Booking Algorithm — Three Phases

### Phase 1: scan occupied rooms before the first idle room

```
idleCandidate = freeRooms.isEmpty() ? null : freeRooms.first()   // O(log R) snapshot

toCheck = (idleCandidate != null)
        ? occupiedRooms.headSet(idleCandidate, exclusive)         // bounded scan
        : occupiedRooms                                           // all rooms occupied

for each roomId in toCheck (lex order):
    lock(roomId)
    if isRoomFree(roomId, start, end):
        insert into roomSchedules
        activeMeetings.putIfAbsent(meetingId, info)   // atomic duplicate-id guard
        if success → return roomId
        else → rollback roomSchedules entry, return ""  // duplicate meetingId
    unlock(roomId)
```

**Why this is bounded:** `headSet(idleCandidate)` returns only occupied rooms lex-smaller than the first idle room. In a typical workload (few rooms busy), this set is small — often zero, meaning Phase 1 does nothing and Phase 2 runs immediately.

### Phase 2: book the idle candidate (the common fast path)

```
if idleCandidate != null:
    lock(idleCandidate)
    if roomSchedules[idleCandidate].isEmpty():         // verify: still idle?
        insert interval
        putIfAbsent meetingId
        if success:
            freeRooms.remove(idleCandidate)
            occupiedRooms.add(idleCandidate)
            return idleCandidate
        else → rollback, return ""
    else:
        // Stale snapshot: another thread booked this room between our freeRooms.first()
        // call and this lock acquisition. Fix the sets and fall through to Phase 3.
        freeRooms.remove(idleCandidate)
        occupiedRooms.add(idleCandidate)
    unlock(idleCandidate)
```

**Why a stale-check is needed:** `freeRooms.first()` is read *without* a lock (it's a snapshot). The window between that read and `roomLocks[idx].lock()` is small but non-zero. Another thread can book `idleCandidate` in that window. We detect this by checking `roomSchedules[idx].isEmpty()` under the lock and fall through if it's no longer empty.

### Phase 3: full linear fallback (rare)

```
for each roomId in roomIds (lex order):
    lock → check → book if free → return
```

Triggered only when:
- `idleCandidate` was concurrently stolen (narrow race window, amortised rare), OR
- Every room has at least one meeting (requires ≥50,000 concurrent active meetings — well beyond typical load)

---

## Overlap Check — `isRoomFree`

The schedule is a sorted map `startTime → endTime`. For a new meeting `[start, end]`:

```
floor = schedule.floorEntry(start)    // last meeting that starts ≤ start
if floor != null && floor.endTime >= start:
    return false   // that meeting extends into our window (inclusive: endTime == start counts)

ceil = schedule.ceilingEntry(start)   // first meeting that starts ≥ start
if ceil != null && ceil.startTime <= end:
    return false   // that meeting begins within our window

return true
```

Two checks, two map lookups: O(log M) total. This correctly handles inclusive endpoints — `floor.endTime >= start` (not `>`) catches the `[1,5]` vs `[5,8]` conflict.

---

## Cancellation

```
info = activeMeetings.get(meetingId)    // O(1) — no lock needed for read
if info == null → return false

lock(info.roomId)
    if activeMeetings.remove(meetingId, info):    // atomic compare-and-remove
        roomSchedules[roomIndex].remove(info.startTime)
        if schedule is now empty:
            occupiedRooms.remove(roomId)
            freeRooms.add(roomId)
        return true
    return false   // concurrent cancel won the race
unlock
```

`ConcurrentHashMap.remove(k, v)` is atomic — it only removes if the current value equals `v`. If two threads race to cancel the same `meetingId`, exactly one succeeds; the other gets `false` safely.

---

## Complexity

| Operation | Best | Average | Worst |
|-----------|------|---------|-------|
| `bookMeeting` | O(log R) — lex-smallest room is idle | O(K·log M + log R), K = occupied rooms before first idle | O(R·log M) — all rooms occupied |
| `cancelMeeting` | O(log R + log M) | O(log R + log M) | O(log R + log M) |
| Space | O(R + N) — R rooms, N active meetings | | |

In practice K ≪ R: with 100,000 total ops across 50,000 rooms, the average room sees ≤ 2 ops in its lifetime. At any snapshot, few rooms are occupied simultaneously.

---

## Thread Safety Summary

| Concern | Mechanism |
|---------|-----------|
| Two threads booking the same room | Per-room `ReentrantLock` — only one thread can check-and-insert |
| Duplicate `meetingId` across threads | `putIfAbsent` is atomic; loser rolls back the schedule entry |
| Two threads cancelling the same meeting | `remove(k, v)` is atomic; only one succeeds |
| `freeRooms` read outside a lock (snapshot) | Stale reads are safe — they only trigger Phase 3 fallback, never a correctness violation |
| Iterating `occupiedRooms.headSet(...)` during mutations | `ConcurrentSkipListSet` weakly-consistent iterators — no exception; may miss a recently-added room, but that room will be checked under its own lock before committing |
| Deadlock | Each thread holds at most one room lock at any time |

---

## Why Not a Segment Tree Over Rooms?

A segment tree over room indices [0..R-1] could potentially find "first available room for [s, e]" faster. The problem: "available" is *query-dependent* (it depends on `[s, e]`), so we cannot precompute a static availability bit per node. The tree would still need to descend to every leaf in the worst case — O(R · log M), same as the linear scan, but with higher constant factors and significantly more complex code.

The three-phase approach achieves sub-linear *practical* cost without that complexity.

---

## Potential Further Improvements

- **Striped locks**: Replace one lock per room with a fixed pool (e.g., 256 locks) assigned by `roomId.hashCode() % 256`. Reduces memory from O(R) locks to O(256). Slightly increases contention per lock but negligible at R = 50k.
- **Time-bucketed room index**: If booking patterns cluster by time window (e.g., business hours), maintain per-bucket sorted room sets. A booking query only scans rooms with meetings in the relevant bucket — reduces Phase 1 scope further.
- **Past-meeting eviction**: `roomSchedules` accumulates ended meetings indefinitely. In production, a background thread or lazy eviction (on access, remove entries where `endTime < now`) would keep schedule sizes bounded.
