package BookMyShow;

import java.util.List;

/**
 * Observer interface for Show seat events.
 * Implementations are notified whenever seats are booked or released.
 */
public interface ShowObserver {
    void onSeatsBooked(int showId, String ticketId, List<String> seats);
    void onSeatsReleased(int showId, String ticketId, List<String> seats);
}
