# BookMyShow — Low Level Design Notes

## Problem Summary

A movie ticket booking system where:
- Cities contain multiple Cinemas
- Each Cinema has multiple Screens with a fixed seat grid (rows × cols)
- A Show is a movie screening on a specific Screen at a specific time
- Users can book/cancel tickets; seats are allocated by a priority algorithm

---

## Class Diagram

```
Solution
  ├── Map<cinemaId, Cinema>
  ├── Map<cityId, List<Cinema>>
  ├── Map<showId, Show>
  └── Map<ticketId, Show>           ← for O(1) cancel lookup

Cinema
  ├── cinemaId, cityId
  ├── Map<screenIndex, Screen>
  └── Map<movieId, List<Show>>      ← index for listShows / hasMovie

Screen
  ├── screenIndex (1-based)
  ├── rows, cols
  └── Seat[rows][cols]              ← mutable seat grid

Show  ← Observable
  ├── showId, movieId, cinemaId
  ├── Screen (reference)
  ├── startTime, endTime
  ├── freeSeats (running counter)
  ├── Map<ticketId, Ticket>
  └── List<ShowObserver>            ← registered observers

Ticket
  ├── ticketId, showId
  ├── List<String> seats            ← immutable seat labels e.g. "2-5"
  └── boolean cancelled

Seat
  ├── row, col
  └── SeatStatus (AVAILABLE / BOOKED)

ShowObserver          ← interface
BookingLogger         ← concrete observer (logs via Helper10)
```

---

## Design Patterns

### Observer Pattern

**Why:** Booking and cancellation are events that multiple consumers may care about
(logging, analytics, notifications, inventory sync) without `Show` needing to know about them.

```
<<interface>>
ShowObserver
+ onSeatsBooked(showId, ticketId, seats)
+ onSeatsReleased(showId, ticketId, seats)
        ▲
        │
BookingLogger          ← logs to Helper10; attached at addShow() time
```

`Show` is the **Observable**. It holds a `List<ShowObserver>` and fires events
after every successful `bookSeats` / `cancelTicket`. Adding a new observer
(e.g. email service, Kafka producer) requires zero changes to `Show`.

---

## Seat Selection Algorithm

Implemented in `Show.selectSeats(int count)`. Two-pass approach:

### Pass 1 — Consecutive seats in a single row
```
for each row r = 0 → rows-1:
    sliding window tracking a run of AVAILABLE seats
    if run length == count:
        return seats [r][runStart] … [r][runStart+count-1]
```
Scans rows top-to-bottom and columns left-to-right, so the first valid
block found is always the one with the lowest row and lowest starting column.

### Pass 2 — Scatter pick (fallback)
```
row-major scan: collect the first `count` AVAILABLE seats
```
Used only when no single row has enough consecutive seats.
Produces the lexicographically smallest set of seats.

### Complexity
- Best case (consecutive found early): O(rows × cols)
- Worst case (scatter): O(rows × cols)
- Cancel: O(k) where k = seats in the ticket

---

## Key Data-Flow

### addCinema
```
Solution → creates Cinema (builds Screen objects internally) → stored in cinemas + cityCinemas maps
```

### addShow
```
Solution → looks up Cinema + Screen → creates Show → attaches BookingLogger observer
         → stored in shows map + cinema.showsByMovie index
```

### bookTicket
```
Solution.bookTicket(ticketId, showId, count)
  → Show.bookSeats(ticketId, count)
      → check freeSeats >= count
      → selectSeats(count)          ← 2-pass algorithm
      → mark seats BOOKED
      → create Ticket, store in show.tickets
      → decrement freeSeats
      → notifyBooked → BookingLogger.onSeatsBooked
  → store ticketId → Show in Solution.ticketToShow
```

### cancelTicket
```
Solution.cancelTicket(ticketId)
  → O(1) lookup: ticketToShow.get(ticketId)
  → Show.cancelTicket(ticketId)
      → check ticket exists and not already cancelled
      → mark each seat AVAILABLE
      → ticket.cancelled = true
      → increment freeSeats
      → notifyReleased → BookingLogger.onSeatsReleased
```

### listCinemas(movieId, cityId)
```
cityCinemas.get(cityId)
  → filter by cinema.hasMovie(movieId)   ← O(1) per cinema via showsByMovie map
  → sort by cinemaId ascending
```

### listShows(movieId, cinemaId)
```
cinema.getShowsForMovie(movieId)         ← O(1) lookup via showsByMovie map
  → sort: startTime DESC, showId ASC
  → return showIds
```

---

## Design Decisions

| Decision | Rationale |
|---|---|
| `Screen` shared by reference in `Show` | No seat duplication; Show mutates the grid directly |
| `ticketToShow` map in `Solution` | O(1) cancel without scanning all shows |
| `showsByMovie` index in `Cinema` | O(1) show lookup per cinema; decouples Cinema from global show registry |
| Running `freeSeats` counter in `Show` | O(1) `getFreeSeatsCount`; avoid O(rows×cols) scan on every query |
| Observer attached at `addShow` time | Clean separation; Solution owns wiring, Show owns notification |
| `Ticket.seats` is unmodifiable | Prevents accidental mutation of the booking record after creation |

---

## Constraints & Assumptions

- Single-threaded environment — no concurrency handling needed
- `screenIndex` is 1-based (as per the problem statement)
- A Show owns a dedicated Screen — no two Shows share a Screen simultaneously
- Seat labels are `"row-col"` strings (e.g. `"0-4"`)
- `cancelTicket` is idempotent-safe: returns `false` on repeat calls
- `bookTicket` returns empty list (no partial booking) if fewer than `ticketsCount` seats are free
