package BookMyShow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entry point for the BookMyShow LLD.
 *
 * Design patterns used:
 *   - Observer  : Show (Observable) notifies ShowObserver implementations
 *                 (BookingLogger) on every seat-booking and cancellation event.
 *
 * All IDs are in the ranges given by the problem statement; no concurrency
 * handling is required (single-threaded environment).
 */
public class Solution {

    private Helper10 helper;

    // Primary lookup tables
    private final Map<Integer, Cinema>       cinemas      = new HashMap<>(); // cinemaId → Cinema
    private final Map<Integer, List<Cinema>> cityCinemas  = new HashMap<>(); // cityId   → [Cinema]
    private final Map<Integer, Show>         shows        = new HashMap<>(); // showId   → Show
    private final Map<String, Show>          ticketToShow = new HashMap<>(); // ticketId → Show

    // -----------------------------------------------------------------------
    // Initializer (acts as constructor)
    // -----------------------------------------------------------------------

    public void init(Helper10 helper) {
        this.helper = helper;
    }

    // -----------------------------------------------------------------------
    // Cinema & Show setup
    // -----------------------------------------------------------------------

    /**
     * Registers a new cinema in the given city with `screenCount` identical screens.
     */
    public void addCinema(int cinemaId, int cityId,
                          int screenCount, int screenRow, int screenColumn) {
        Cinema cinema = new Cinema(cinemaId, cityId, screenCount, screenRow, screenColumn);
        cinemas.put(cinemaId, cinema);
        cityCinemas.computeIfAbsent(cityId, k -> new ArrayList<>()).add(cinema);
    }

    /**
     * Creates a Show for a movie on a specific screen of the given cinema.
     * A BookingLogger observer is automatically attached to every new Show.
     */
    public void addShow(int showId, int movieId, int cinemaId,
                        int screenIndex, long startTime, long endTime) {
        Cinema cinema = cinemas.get(cinemaId);
        if (cinema == null) return;

        Screen screen = cinema.getScreen(screenIndex);
        if (screen == null) return;

        Show show = new Show(showId, movieId, cinemaId, screen, startTime, endTime);

        // Attach the logging observer (Observer Pattern – Open/Closed: add more observers here)
        show.addObserver(new BookingLogger(helper));

        shows.put(showId, show);
        cinema.addShow(show);
    }

    // -----------------------------------------------------------------------
    // Booking operations
    // -----------------------------------------------------------------------

    /**
     * Books `ticketsCount` seats for the given show.
     *
     * Booking Criteria (delegated to Show.bookSeats):
     *   1. Prefer consecutive seats in the same row, starting from lowest row/col.
     *   2. Fall back to scatter pick (lowest row then col) if no consecutive block exists.
     *
     * @return list of seat labels (e.g. ["0-0","0-1"]), or empty list if booking failed.
     */
    public List<String> bookTicket(String ticketId, int showId, int ticketsCount) {
        Show show = shows.get(showId);
        if (show == null) return new ArrayList<>();

        List<String> booked = show.bookSeats(ticketId, ticketsCount);
        if (!booked.isEmpty()) {
            ticketToShow.put(ticketId, show);
        }
        return booked;
    }

    /**
     * Cancels a ticket and releases its seats back to the show.
     *
     * @return true if the ticket existed and was not already cancelled; false otherwise.
     */
    public boolean cancelTicket(String ticketId) {
        Show show = ticketToShow.get(ticketId);
        if (show == null) return false;

        return show.cancelTicket(ticketId);
    }

    // -----------------------------------------------------------------------
    // Query operations
    // -----------------------------------------------------------------------

    /**
     * Returns the number of available seats for the given show.
     * Returns 0 if the show does not exist.
     */
    public int getFreeSeatsCount(int showId) {
        Show show = shows.get(showId);
        return show == null ? 0 : show.getFreeSeatsCount();
    }

    /**
     * Lists all cinemas in a city that are running at least one show for the given movie.
     * Cinema IDs are returned in ascending order.
     */
    public List<Integer> listCinemas(int movieId, int cityId) {
        List<Cinema> cityList = cityCinemas.getOrDefault(cityId, Collections.emptyList());

        return cityList.stream()
                .filter(c -> c.hasMovie(movieId))
                .map(c -> c.cinemaId)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Lists all shows in a cinema for the given movie.
     * Ordered by startTime descending; ties broken by showId ascending.
     */
    public List<Integer> listShows(int movieId, int cinemaId) {
        Cinema cinema = cinemas.get(cinemaId);
        if (cinema == null) return new ArrayList<>();

        return cinema.getShowsForMovie(movieId).stream()
                .sorted(Comparator
                        .comparingLong(Show::getStartTime).reversed()
                        .thenComparingInt(s -> s.showId))
                .map(s -> s.showId)
                .collect(Collectors.toList());
    }
}
