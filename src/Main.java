import HitCounter.HitCounter;

public class Main {
    public static void main(String[] args) {
        testHitCounter();
        testChess();
    }

    static void testHitCounter() {
        System.out.println("=== HitCounter ===");
        HitCounter counter = new HitCounter();
        counter.init(3);
        counter.incrementVisitCount(0);
        counter.incrementVisitCount(0);
        counter.incrementVisitCount(1);
        System.out.println("Page 0: " + counter.getVisitCount(0));  // 2
        System.out.println("Page 1: " + counter.getVisitCount(1));  // 1
        System.out.println("Page 2: " + counter.getVisitCount(2));  // 0
    }

    static void testChess() {
        System.out.println("\n=== Chess ===");
        Chess.Solution chess = new Chess.Solution();
        String[][] board = new String[8][8];
        board[0][0] = "WR";  // White Rook
        board[7][0] = "BR";  // Black Rook
        board[0][4] = "WK";  // White King
        board[7][4] = "BK";  // Black King
        chess.init(board);
        // White Rook moves up the a-file
        System.out.println("WR (0,0)->(4,0): " + chess.move(0, 0, 4, 0));  // ""
        // Black Rook captures White Rook
        System.out.println("BR (7,0)->(4,0): " + chess.move(7, 0, 4, 0));  // "WR"
        // White King steps sideways
        System.out.println("WK (0,4)->(0,3): " + chess.move(0, 4, 0, 3));  // ""
        System.out.println("Game status:      " + chess.getGameStatus());   // 0
    }
}
