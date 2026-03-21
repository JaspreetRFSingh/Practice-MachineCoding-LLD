package Chess;

// Same row or same column; no piece may stand in the way.
class Rook extends Piece {
    @Override
    boolean isLegalMove(int r1, int c1, int r2, int c2, char color, String[][] board) {
        if (r1 != r2 && c1 != c2) return false;
        return isPathClear(r1, c1, r2, c2, board);
    }
}
