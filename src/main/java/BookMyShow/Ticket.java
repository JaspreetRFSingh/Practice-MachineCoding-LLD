package BookMyShow;

import java.util.Collections;
import java.util.List;

/**
 * Immutable booking record created when seats are successfully reserved.
 */
public class Ticket {

    final String       ticketId;
    final int          showId;
    final List<String> seats;      // seat labels, e.g. ["0-0", "0-1"]
    boolean            cancelled;

    public Ticket(String ticketId, int showId, List<String> seats) {
        this.ticketId  = ticketId;
        this.showId    = showId;
        this.seats     = Collections.unmodifiableList(seats);
        this.cancelled = false;
    }
}
