# ShoppingCart — Low Level Design Notes

## Problem Summary

An in-memory shopping cart backed by an item catalog. Supports:
- Adding items to a cart with stock validation
- Viewing cart contents
- Checkout: commits stock deductions and returns total cost
- Error cases: unknown item, insufficient stock, empty-cart checkout

---

## Class Diagram

```
ShoppingCart
  ├── Map<itemId, CatalogItem>    ← immutable keys; mutable stock
  └── Map<itemId, Integer>        ← cart: itemId → reserved count

CatalogItem
  ├── itemId         (final)
  ├── pricePerUnit   (final)
  └── availableUnits (mutable — decremented only at checkout)
```

Two classes only. No `CartItem` wrapper class — the cart is a `Map<String, Integer>` because the only cart-level data is a count. Introducing a class for a single integer field would be over-engineering.

---

## Key Design Decisions

### 1. Stock reservation model (most important decision)

**Problem:** When does stock get deducted — at `addItem` or at `checkout`?

**Choice:** Stock is **not** deducted at `addItem`. It is only deducted at `checkout`. However, the stock check at `addItem` is against `availableUnits - alreadyInCart` to prevent over-committing.

```
stockCheck = cart.getOrDefault(itemId, 0) + newCount <= catalog.availableUnits
```

**Why this matters:** If stock were decremented at `addItem`, an abandoned cart would permanently reduce available inventory. The reservation model keeps catalog stock authoritative until the transaction is committed.

**Alternative considered:** Decrement at `addItem`, restore at removal or cart abandonment. Rejected — this requires tracking removals and session expiry, which adds complexity with no benefit in a single-user in-memory system.

---

### 2. `viewCart` sorts at call time, not at insertion time

The cart is stored in a `HashMap` (O(1) insert/lookup). Sorting is deferred to `viewCart` — O(n log n) where n = distinct items in cart. At cart sizes, this is negligible.

**Rejected alternative:** `TreeMap` for the cart. Gives sorted iteration for free but costs O(log n) on every `addItem`. The viewCart call is far less frequent than addItem, so lazy sorting is better.

---

### 3. `CatalogItem.deductUnits` is the only mutation point

Stock changes happen in exactly one place: `checkout` calls `item.deductUnits(count)`. This makes the state transition easy to audit — if stock is wrong, there is only one code path to inspect. `CatalogItem` exposes no setter for arbitrary mutation.

---

### 4. Constants for response strings

`SUCCESS`, `UNAVAILABLE`, `OUT OF STOCK` are `private static final` constants in `ShoppingCart`. This prevents typos and makes the valid return values the first thing a reviewer sees when reading the class.

---

## Data Flow

### `addItem(itemId, count)`
```
1. catalog.get(itemId) == null → return UNAVAILABLE
2. alreadyInCart = cart.getOrDefault(itemId, 0)
3. alreadyInCart + count > item.availableUnits → return OUT OF STOCK
4. cart.put(itemId, alreadyInCart + count)
5. return SUCCESS
```

### `viewCart()`
```
1. cart is empty → return []
2. sort cart.keySet() lexicographically
3. build "itemId,count" rows
```

### `checkout()`
```
1. cart is empty → return -1
2. for each (itemId, count) in cart:
     total += catalog.get(itemId).pricePerUnit * count
     catalog.get(itemId).deductUnits(count)     ← commit stock
3. cart.clear()
4. return total
```

---

## Complexity

| Operation | Time | Space |
|---|---|---|
| `addItem` | O(1) | O(1) |
| `viewCart` | O(n log n) | O(n) |
| `checkout` | O(n) | O(1) |

n = number of distinct items in cart.

---

## Constraints & Assumptions

- Single-threaded; no concurrent cart access
- `addItem` with an existing item accumulates count (not a replace)
- `checkout` does not reset catalog stock to initial values — it permanently deducts
- `checkout` on an empty cart returns -1 and leaves catalog unchanged
- Input parsing assumes well-formed CSV rows (no validation needed per problem spec)
