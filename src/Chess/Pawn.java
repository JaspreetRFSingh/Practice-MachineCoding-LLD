package Chess;

// Pawn is the only piece whose validity depends on whether the destination is empty or occupied.
// forward = +1 for white (toward row 7), -1 for black (toward row 0).
class Pawn extends Piece {
    @Override
    boolean isLegalMove(int r1, int c1, int r2, int c2, char color, String[][] board) {
        int forward = (color == 'W') ? 1 : -1;
        int dr = r2 - r1, dc = c2 - c1;

        if (dr == forward && dc == 0)           return board[r2][c2].isEmpty();   // straight → must be empty
        if (dr == forward && Math.abs(dc) == 1) return !board[r2][c2].isEmpty();  // diagonal → must capture
        return false;
    }
}
