package BookMyShow;

public class Seat {

    final int row;
    final int col;
    SeatStatus status;

    public Seat(int row, int col) {
        this.row    = row;
        this.col    = col;
        this.status = SeatStatus.AVAILABLE;
    }

    /** Returns the seat label used in API responses, e.g. "2-5". */
    public String label() {
        return row + "-" + col;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }
}
