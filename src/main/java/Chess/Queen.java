package Chess;

// Queen = Rook ∪ Bishop. Composes both rather than duplicating logic.
class Queen extends Piece {
    private final Rook   rook   = new Rook();
    private final Bishop bishop = new Bishop();

    @Override
    boolean isLegalMove(int r1, int c1, int r2, int c2, char color, String[][] board) {
        return rook.isLegalMove(r1, c1, r2, c2, color, board)
            || bishop.isLegalMove(r1, c1, r2, c2, color, board);
    }
}
