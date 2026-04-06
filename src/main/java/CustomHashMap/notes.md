# CustomHashMap — Low Level Design Notes

## Problem Summary

An in-memory HashMap (String → String) built from scratch:
- Custom hash function
- Separate chaining for collision handling
- Dynamic resizing (grow and shrink) driven by a load factor

No built-in Map, Set, or Dictionary may be used internally.

---

## Class Diagram

```
CustomHashMap
  ├── Entry[] buckets          ← array of bucket heads (linked list per bucket)
  ├── int size                 ← live entry count
  ├── double minLoadFactor
  └── double maxLoadFactor

Entry  (private static inner class — linked list node)
  ├── String key   (final)
  ├── String value (mutable — updated on key collision)
  └── Entry next
```

A single file / two classes. `Entry` is a private static inner class — it has no need for a reference to the outer object, so `static` avoids the hidden reference and the memory overhead that comes with it.

---

## Hash Function

```
hash(key) = (key.length)^2 + Σ charValue(c)    where a=1, b=2, …, z=26
bucketIndex = hash(key) % bucketsCount
```

Always non-negative: `length^2 ≥ 1` and each `charValue ≥ 1`, so no modulo of a negative number can occur.

In a production HashMap (e.g. Java's) you would also apply a secondary mixing step (e.g. `(h ^ (h >>> 16))`) to spread bits better and reduce clustering. Omitted here per the problem spec.

---

## Collision Handling — Separate Chaining

Each bucket is the **head of a singly-linked list** of `Entry` nodes. On collision (two keys map to the same bucket index), the new entry is prepended — O(1) insertion.

**Why prepend, not append?**
Prepending keeps insertion O(1) without a tail pointer. Under good load factor control (LF ≤ 0.75) the average chain length stays near 1, so appending would offer no practical benefit.

**Alternative considered: open addressing (linear probing).**
Rejected because it complicates removal (requires tombstones or backward shift) and degrades badly as LF approaches 1. Separate chaining degrades gracefully and removal is straightforward pointer surgery.

---

## Load Factor & Rehashing

```
LoadFactor = round2(size / bucketsCount)
```

Rounded to 2 decimal places consistently — the same `round2` utility is applied to constructor params, each LF computation, and each threshold comparison. This prevents floating-point drift from causing spurious rehash triggers.

### Grow (LF strictly > maxLF)
```
newCount = bucketsCount * 2
while round2(size / newCount) > maxLF:
    newCount *= 2
rehash(newCount)
```
Triggered after a size-increasing `put`. A loop handles the edge case where a single doubling is still not enough (e.g. after a large batch insert into a tiny map).

### Shrink (LF strictly < minLF, floor = 2 buckets)
```
newCount = bucketsCount / 2
while newCount > 2 AND round2(size / newCount) < minLF:
    newCount /= 2
rehash(newCount)
```
Triggered after `remove`. Shrinking reclaims memory but never goes below 2 buckets — halving to 1 would degenerate into a single linked list.

### Rehash implementation
```java
for each bucket:
    for each Entry node in chain:
        newIdx = hash(key) % newBucketsCount
        prepend node to newBuckets[newIdx]
```
Existing `Entry` nodes are **reused** — only their `next` pointers are rewired. No new `Entry` allocation during rehash. O(n) time, O(newBucketsCount) space for the new array.

---

## Update vs Insert in `put`

When a key already exists, `put` updates the value in-place and **returns immediately without calling `checkRehash`**. This is intentional:
- Size is unchanged → LF is unchanged → no rehash can trigger.
- Skipping the check avoids a redundant load factor computation on every update.

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Separate chaining (linked list) | Simpler removal than open addressing; no tombstones; degrades gracefully |
| Prepend new entries to bucket chain | O(1) insertion without a tail pointer |
| Reuse Entry nodes during rehash | Zero allocation during resize — just pointer rewiring; cache-friendlier |
| `Entry` is `private static` inner class | No hidden outer reference; reduces per-node memory by one pointer |
| `round2` applied uniformly | Prevents floating-point drift from causing spurious or missed rehash triggers |
| Rehash checked only on size-changing operations | Update (`put` on existing key) cannot change LF; checking would be wasted work |
| Floor at 2 buckets on shrink | Prevents degeneration to a single linked list on excessive removes |
| `getBucketKeys` sorts on demand | Keys are not kept sorted inside the bucket (that would cost O(n) per insert for no benefit at lookup time); sorted output is a view concern |

---

## Complexity

| Operation | Average | Worst Case |
|---|---|---|
| `put` (new key) | O(1) amortised | O(n) — rehash |
| `put` (update) | O(1) | O(n) — long chain |
| `get` | O(1) | O(n) — long chain |
| `remove` | O(1) amortised | O(n) — rehash |
| `getBucketKeys` | O(k log k) | O(n log n) |
| `rehash` | O(n) | O(n) |

n = total entries, k = entries in the queried bucket.
Average-case O(1) holds when load factor is bounded — which this implementation guarantees by design.

---

## Constraints & Assumptions

- Keys are lowercase `a-z` strings, length 1–20; values length 1–20
- Single-threaded — no concurrent access; no synchronisation needed
- Load factor parameters are rounded to 2 decimals at construction time
- `put` on an existing key is a value update — size and load factor are unaffected
- Shrink floor is 2 buckets; initial capacity is also 2
- Timestamps and ordering of rehash: checked immediately after `size` changes
