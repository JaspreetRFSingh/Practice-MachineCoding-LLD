package Chess;

// L-shape: (±2,±1) or (±1,±2). Only piece that jumps — no path check needed.
class Knight extends Piece {
    @Override
    boolean isLegalMove(int r1, int c1, int r2, int c2, char color, String[][] board) {
        int dr = Math.abs(r2 - r1), dc = Math.abs(c2 - c1);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }
}
