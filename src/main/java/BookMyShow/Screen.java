package BookMyShow;

/**
 * Represents one physical screen inside a Cinema.
 * Holds the 2-D seat grid; seat state is mutated by Show during booking.
 */
public class Screen {

    final int screenIndex; // 1-based, as supplied by addShow()
    final int rows;
    final int cols;
    final Seat[][] seats;

    public Screen(int screenIndex, int rows, int cols) {
        this.screenIndex = screenIndex;
        this.rows        = rows;
        this.cols        = cols;
        this.seats       = new Seat[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                seats[r][c] = new Seat(r, c);
            }
        }
    }
}
