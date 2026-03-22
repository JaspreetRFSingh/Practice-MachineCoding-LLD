package BookMyShow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a physical cinema located in a city.
 * Owns multiple Screens and maintains an index of Shows grouped by movie.
 */
public class Cinema {

    final int cinemaId;
    final int cityId;

    // 1-based screenIndex → Screen (indices 1 .. screenCount)
    private final Map<Integer, Screen> screens = new HashMap<>();

    // movieId → list of Shows running in this cinema
    private final Map<Integer, List<Show>> showsByMovie = new HashMap<>();

    public Cinema(int cinemaId, int cityId,
                  int screenCount, int screenRow, int screenColumn) {
        this.cinemaId = cinemaId;
        this.cityId   = cityId;

        for (int idx = 1; idx <= screenCount; idx++) {
            screens.put(idx, new Screen(idx, screenRow, screenColumn));
        }
    }

    // -----------------------------------------------------------------------
    // Screen access
    // -----------------------------------------------------------------------

    /** Returns the Screen at 1-based screenIndex, or null if out of range. */
    public Screen getScreen(int screenIndex) {
        return screens.get(screenIndex);
    }

    // -----------------------------------------------------------------------
    // Show management
    // -----------------------------------------------------------------------

    public void addShow(Show show) {
        showsByMovie.computeIfAbsent(show.movieId, k -> new ArrayList<>()).add(show);
    }

    /** Returns all shows for a movie in this cinema; empty list if none. */
    public List<Show> getShowsForMovie(int movieId) {
        return showsByMovie.getOrDefault(movieId, new ArrayList<>());
    }

    /** True if this cinema has at least one show for the given movie. */
    public boolean hasMovie(int movieId) {
        List<Show> list = showsByMovie.get(movieId);
        return list != null && !list.isEmpty();
    }
}
