package Chess;

// Strategy interface — each subclass owns the move rules for one piece type.
// isPathClear is protected here because Rook, Bishop, and Queen all need it.
abstract class Piece {

    abstract boolean isLegalMove(int r1, int c1, int r2, int c2, char color, String[][] board);

    // Walks every square strictly between (r1,c1) and (r2,c2).
    // Covers horizontal, vertical, and diagonal paths via Integer.signum.
    protected boolean isPathClear(int r1, int c1, int r2, int c2, String[][] board) {
        int dr = Integer.signum(r2 - r1);
        int dc = Integer.signum(c2 - c1);
        int r = r1 + dr, c = c1 + dc;
        while (r != r2 || c != c2) {
            if (!board[r][c].isEmpty()) return false;
            r += dr; c += dc;
        }
        return true;
    }
}
