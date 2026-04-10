# Machine Coding / Low Level Design Practice

A collection of LLD problems implemented in Java.

---

## Problems

| # | Problem | Description | Design Pattern(s) |
|---|---------|-------------|-------------------|
| 1 | [BookMyShow](src/main/java/BookMyShow/notes.md) | Movie ticket booking system — cinemas, screens, shows, and seat allocation with an observer-based booking log | Observer |
| 2 | [Chess](src/main/java/Chess/notes.md) | Two-player chess engine with legal move validation, piece capture, and game-status tracking | Factory |
| 3 | [ElevatorSystem](src/main/java/ElevatorSystem/notes.md) | Multi-lift elevator controller — assigns lift requests by proximity/direction and advances state on each `tick()` | State |
| 4 | [HitCounter](src/main/java/HitCounter/notes.md) | Thread-safe per-page visit counter backed by atomic integers | — |
| 5 | [JobScheduler](src/main/java/JobScheduler/notes.md) | Distributed job scheduler — assigns jobs to capability-matched machines using pluggable selection strategies | Strategy |
| 6 | [MeetingRoomScheduler](src/main/java/MeetingRoomScheduler/notes.md) | Conference room booking system — prevents overlapping reservations and assigns the lexicographically smallest free room | — |
| 7 | [ParkingLot](src/main/java/ParkingLot/notes.md) | Multi-floor parking lot with 2W/4W spot types and pluggable parking strategies | Strategy, Factory |
| 8 | [Splitwise](src/main/java/splitwise/notes.md) | Expense splitting app — tracks net balances across users and resolves who owes whom | — |
| 9 | [RateLimiter](src/main/java/RateLimiter/notes.md) | In-memory rate limiter with pluggable algorithms (fixed-window, sliding-window) per resource, reconfigurable at runtime | Strategy |
| 10 | [ShoppingCart](src/main/java/ShoppingCart/notes.md) | In-memory shopping cart with catalog-backed stock reservation, lazy-sort view, and atomic checkout | — |
| 11 | [CustomHashMap](src/main/java/CustomHashMap/notes.md) | HashMap built from scratch — separate chaining, custom hash, and load-factor-driven grow/shrink rehashing | — |
| 12 | [CustomerIssueResolution](src/main/java/CustomerIssueResolution/notes.md) | Multi-threaded customer support system — assigns issues to agents by expertise using pluggable selection strategies | Strategy |
| 13 | [PubSubSystem](src/main/java/PubSubSystem/notes.md) | Single-queue in-memory pub/sub — subscribers filter by event type, consume independently, counts accumulate across sessions | Observer |
