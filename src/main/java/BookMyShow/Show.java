package BookMyShow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Observable: a particular movie screening on a specific Screen.
 *
 * Seat-selection algorithm (Booking Criteria):
 *   1. Try to find `count` consecutive seats within a single row,
 *      scanning from row 0 → last row, col 0 → last col.
 *   2. If no such run exists, scatter-pick available seats in
 *      row-major order (lowest row then lowest col).
 *
 * ShowObservers are notified after every successful booking or cancellation.
 */
public class Show {

    final int    showId;
    final int    movieId;
    final int    cinemaId;
    final Screen screen;
    final long   startTime;
    final long   endTime;

    private int freeSeats;

    // ticketId → Ticket
    private final Map<String, Ticket> tickets = new HashMap<>();

    // Registered observers (Observer Pattern)
    private final List<ShowObserver> observers = new ArrayList<>();

    public Show(int showId, int movieId, int cinemaId,
                Screen screen, long startTime, long endTime) {
        this.showId    = showId;
        this.movieId   = movieId;
        this.cinemaId  = cinemaId;
        this.screen    = screen;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.freeSeats = screen.rows * screen.cols;
    }

    // -----------------------------------------------------------------------
    // Observer management
    // -----------------------------------------------------------------------

    public void addObserver(ShowObserver observer) {
        observers.add(observer);
    }

    private void notifyBooked(String ticketId, List<String> seats) {
        for (ShowObserver o : observers) {
            o.onSeatsBooked(showId, ticketId, seats);
        }
    }

    private void notifyReleased(String ticketId, List<String> seats) {
        for (ShowObserver o : observers) {
            o.onSeatsReleased(showId, ticketId, seats);
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public int getFreeSeatsCount() {
        return freeSeats;
    }

    /**
     * Attempts to book `count` seats for the given ticketId.
     *
     * @return list of booked seat labels, or empty list if booking failed.
     */
    public List<String> bookSeats(String ticketId, int count) {
        if (freeSeats < count) {
            return new ArrayList<>();
        }

        List<String> chosen = selectSeats(count);

        // Mark chosen seats as BOOKED
        for (String label : chosen) {
            seatByLabel(label).status = SeatStatus.BOOKED;
        }

        Ticket ticket = new Ticket(ticketId, showId, chosen);
        tickets.put(ticketId, ticket);
        freeSeats -= count;

        notifyBooked(ticketId, chosen);
        return chosen;
    }

    /**
     * Cancels a previously booked ticket.
     *
     * @return true if cancelled, false if ticketId unknown or already cancelled.
     */
    public boolean cancelTicket(String ticketId) {
        Ticket ticket = tickets.get(ticketId);
        if (ticket == null || ticket.cancelled) {
            return false;
        }

        for (String label : ticket.seats) {
            seatByLabel(label).status = SeatStatus.AVAILABLE;
        }

        ticket.cancelled = true;
        freeSeats += ticket.seats.size();

        notifyReleased(ticketId, ticket.seats);
        return true;
    }

    // -----------------------------------------------------------------------
    // Seat-selection logic
    // -----------------------------------------------------------------------

    /**
     * Implements the Booking Criteria:
     *   Priority 1 – consecutive seats in the same row (lowest row/col first).
     *   Priority 2 – scatter from lowest row/col if no run available.
     *
     * Returns the seat labels only (does NOT mutate seat state).
     */
    private List<String> selectSeats(int count) {
        // --- Pass 1: look for a consecutive run of `count` in any row ---
        for (int r = 0; r < screen.rows; r++) {
            int runStart = -1;
            int runLen   = 0;

            for (int c = 0; c < screen.cols; c++) {
                if (screen.seats[r][c].isAvailable()) {
                    if (runStart == -1) runStart = c;
                    runLen++;
                    if (runLen == count) {
                        // Found a valid consecutive block
                        List<String> seats = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            seats.add(screen.seats[r][runStart + i].label());
                        }
                        return seats;
                    }
                } else {
                    runStart = -1;
                    runLen   = 0;
                }
            }
        }

        // --- Pass 2: scatter – pick from lowest row/col ---
        List<String> seats = new ArrayList<>(count);
        outer:
        for (int r = 0; r < screen.rows; r++) {
            for (int c = 0; c < screen.cols; c++) {
                if (screen.seats[r][c].isAvailable()) {
                    seats.add(screen.seats[r][c].label());
                    if (seats.size() == count) break outer;
                }
            }
        }
        return seats;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Parse "row-col" label and return the Seat object. */
    private Seat seatByLabel(String label) {
        String[] parts = label.split("-");
        int r = Integer.parseInt(parts[0]);
        int c = Integer.parseInt(parts[1]);
        return screen.seats[r][c];
    }

    public long getStartTime() { return startTime; }
}
