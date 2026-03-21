package Chess;

public class Solution {

    private String[][] board;   // board[row][col]: "WR", "BP", "" for empty
    private int currentTurn;    // 0 = white, 1 = black
    private int gameStatus;     // 0 = in progress, 1 = white won, 2 = black won

    public void init(String[][] chessboard) {
        board = new String[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board[r][c] = (chessboard[r][c] == null) ? "" : chessboard[r][c];
        currentTurn = 0;
        gameStatus  = 0;
    }

    /**
     * Attempts the move for the current player.
     * Returns "invalid" on any rule violation, "" on a non-capturing move,
     * or the captured piece string (e.g. "BK", "WP") on a capture.
     * Capturing the opponent's King ends the game.
     */
    public String move(int startRow, int startCol, int endRow, int endCol) {
        // 1. Game already over?
        if (gameStatus != 0) return "invalid";

        // 2. Coordinates in bounds?
        if (!inBounds(startRow, startCol) || !inBounds(endRow, endCol)) return "invalid";

        // 3. Must actually move
        if (startRow == endRow && startCol == endCol) return "invalid";

        String piece = board[startRow][startCol];

        // 4. Piece present at source?
        if (piece.isEmpty()) return "invalid";

        char color     = piece.charAt(0);
        char type      = piece.charAt(1);
        char turnColor = (currentTurn == 0) ? 'W' : 'B';

        // 5. Belongs to current player?
        if (color != turnColor) return "invalid";

        String target = board[endRow][endCol];

        // 6. Not capturing own piece?
        if (!target.isEmpty() && target.charAt(0) == color) return "invalid";

        // 7. Piece-specific move rules — delegated to the strategy via factory
        if (!PieceFactory.get(type).isLegalMove(startRow, startCol, endRow, endCol, color, board))
            return "invalid";

        // 8. Execute
        String captured = board[endRow][endCol];
        board[endRow][endCol]     = piece;
        board[startRow][startCol] = "";

        // King captured → game over
        if (!captured.isEmpty() && captured.charAt(1) == 'K')
            gameStatus = (currentTurn == 0) ? 1 : 2;

        currentTurn = 1 - currentTurn;
        return captured;
    }

    /** 0 = in progress, 1 = white won, 2 = black won */
    public int getGameStatus() { return gameStatus; }

    /** 0 = white's turn, 1 = black's turn, -1 = game over */
    public int getNextTurn()   { return (gameStatus != 0) ? -1 : currentTurn; }

    private boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }
}
