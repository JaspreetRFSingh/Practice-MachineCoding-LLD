package BookMyShow;

import java.util.List;

/**
 * Concrete Observer: logs every booking and cancellation event.
 */
public class BookingLogger implements ShowObserver {

    private final Helper10 helper;

    public BookingLogger(Helper10 helper) {
        this.helper = helper;
    }

    @Override
    public void onSeatsBooked(int showId, String ticketId, List<String> seats) {
        helper.println("[BOOKING] showId=" + showId
                + " ticketId=" + ticketId
                + " seats=" + seats);
    }

    @Override
    public void onSeatsReleased(int showId, String ticketId, List<String> seats) {
        helper.println("[CANCEL]  showId=" + showId
                + " ticketId=" + ticketId
                + " seats=" + seats);
    }
}
