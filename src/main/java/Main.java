import HitCounter.HitCounter;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        testHitCounter();
        testChess();
        testElevatorSystem();
        testMeetingRoomScheduler();
        testParkingLot();
        testSplitwise();
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

    static void testElevatorSystem() {
        System.out.println("\n=== ElevatorSystem ===");
        elevatorsystem.ElevatorSystem es = new elevatorsystem.ElevatorSystem();
        es.init(10, 2, 5, null);
        int lift = es.requestLift(3, 'U');
        System.out.println("Lift assigned (floor 3, going up): " + lift);
        es.pressFloorButtonInLift(lift, 6);
        es.tick();
        System.out.println("After tick 1: " + es.getLiftState(lift));
        es.tick();
        System.out.println("After tick 2: " + es.getLiftState(lift));
        es.tick();
        System.out.println("After tick 3: " + es.getLiftState(lift));
    }

    static void testMeetingRoomScheduler() {
        System.out.println("\n=== MeetingRoomScheduler ===");
        MeetingRoomScheduler.RoomBooking rb =
                new MeetingRoomScheduler.RoomBooking(List.of("R1", "R2", "R3"));
        System.out.println("Book m1 (10-20): " + rb.bookMeeting("m1", 10, 20));  // R1
        System.out.println("Book m2 (15-25): " + rb.bookMeeting("m2", 15, 25));  // R2
        System.out.println("Book m3 (10-20): " + rb.bookMeeting("m3", 10, 20));  // R3
        System.out.println("Cancel m1:       " + rb.cancelMeeting("m1"));         // true
        System.out.println("Book m4 (10-20): " + rb.bookMeeting("m4", 10, 20));  // R1
    }

    static void testParkingLot() {
        System.out.println("\n=== ParkingLot ===");
        ParkingLot.Solution lot = new ParkingLot.Solution();
        // 1 floor, 2 rows: spots typed as [2W, 4W, 2W] and [4W, 4W, 2W]
        int[][][] parking = {{{2, 4, 2}, {4, 4, 2}}};
        lot.init(parking);
        String s2w = lot.park(2, "MH01AB1234", "T001", 0);
        String s4w = lot.park(4, "MH02CD5678", "T002", 0);
        System.out.println("2W parked at:          " + s2w);                          // 0-0-0
        System.out.println("4W parked at:          " + s4w);                          // 0-0-1
        System.out.println("Search by ticket T001: " + lot.searchVehicle("T001"));    // 0-0-0
        System.out.println("Free 2W (floor 0):     " + lot.getFreeSpotsCount(0, 2)); // 1
        lot.removeVehicle(s2w);
        System.out.println("Free 2W after unpark:  " + lot.getFreeSpotsCount(0, 2)); // 2
    }

    static void testSplitwise() {
        System.out.println("\n=== Splitwise ===");
        splitwise.SplitBook sb = new splitwise.SplitBook();
        sb.registerUser("alice", "Alice");
        sb.registerUser("bob", "Bob");
        sb.registerUser("charlie", "Charlie");
        // Alice pays 90 for all three; each owes 30 → bob and charlie owe alice
        sb.recordExpense(1, List.of("alice", "bob", "charlie"), List.of(90, 0, 0));
        System.out.println("Balances after expense of 90 (paid by alice):");
        sb.listBalances().forEach(System.out::println);
        // Add a second expense: Bob pays 60 for alice and bob; each owes 30
        sb.recordExpense(2, List.of("alice", "bob"), List.of(0, 60));
        System.out.println("Balances after bob pays 60 for alice and bob:");
        sb.listBalances().forEach(System.out::println);
    }
}
