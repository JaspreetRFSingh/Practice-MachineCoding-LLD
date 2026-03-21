package Chess;

// 1 step in any of the 8 directions.
// Same-square exclusion is handled upstream in Solution.move().
class King extends Piece {
    @Override
    boolean isLegalMove(int r1, int c1, int r2, int c2, char color, String[][] board) {
        return Math.abs(r2 - r1) <= 1 && Math.abs(c2 - c1) <= 1;
    }
}
