package Chess;

// Strict diagonal; no piece may stand in the way.
class Bishop extends Piece {
    @Override
    boolean isLegalMove(int r1, int c1, int r2, int c2, char color, String[][] board) {
        if (Math.abs(r2 - r1) != Math.abs(c2 - c1)) return false;
        return isPathClear(r1, c1, r2, c2, board);
    }
}
