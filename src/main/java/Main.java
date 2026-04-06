import HitCounter.HitCounter;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        testHitCounter();
        testChess();
        testElevatorSystem();
        testMeetingRoomScheduler();
        testParkingLot();
        testSplitwise();
        testBookMyShow();
        testJobScheduler();
        testRateLimiter();
        testShoppingCart();
        testCustomHashMap();
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

    static void testBookMyShow() {
        System.out.println("\n=== BookMyShow ===");
        BookMyShow.Solution bms = new BookMyShow.Solution();
        BookMyShow.Helper10 h   = new BookMyShow.Helper10();
        bms.init(h);

        // Cinema 0 in city 1: 4 screens, each 5 rows x 10 cols (50 seats/screen)
        bms.addCinema(0, 1, 4, 5, 10);
        // Cinema 1 in city 1: 2 screens, each 3 rows x 5 cols
        bms.addCinema(1, 1, 2, 3, 5);

        // Shows in cinema 0
        bms.addShow(1, 4,  0, 1, 1710516108725L, 1710523308725L); // movie 4, screen 1
        bms.addShow(2, 11, 0, 3, 1710516108725L, 1710523308725L); // movie 11, screen 3
        bms.addShow(3, 4,  0, 2, 1710519708725L, 1710526908725L); // movie 4, screen 2 (later)

        // Show in cinema 1
        bms.addShow(4, 4,  1, 1, 1710516108725L, 1710523308725L); // movie 4 in cinema 1

        // listCinemas
        System.out.println("Cinemas for movie 0, city 1: " + bms.listCinemas(0, 1)); // []
        System.out.println("Cinemas for movie 4, city 1: " + bms.listCinemas(4, 1)); // [0, 1]
        System.out.println("Cinemas for movie 11, city 1:" + bms.listCinemas(11, 1)); // [0]

        // listShows (descending startTime, then ascending showId)
        System.out.println("Shows for movie 4, cinema 0: " + bms.listShows(4, 0));   // [3, 1]
        System.out.println("Shows for movie 11, cinema 0:" + bms.listShows(11, 0));  // [2]

        // Free seats
        System.out.println("Free seats show 1: " + bms.getFreeSeatsCount(1)); // 50

        // Book 4 consecutive seats → row 0, cols 0-3
        List<String> t1 = bms.bookTicket("tkt-1", 1, 4);
        System.out.println("tkt-1 seats: " + t1);                            // [0-0,0-1,0-2,0-3]
        System.out.println("Free seats show 1: " + bms.getFreeSeatsCount(1)); // 46

        // Book 8 seats (more than remaining in any single row → scatter across rows)
        List<String> t2 = bms.bookTicket("tkt-2", 1, 8);
        System.out.println("tkt-2 seats: " + t2);                            // [0-4..0-9, 1-0, 1-1]
        System.out.println("Free seats show 1: " + bms.getFreeSeatsCount(1)); // 38

        // Cancel tkt-1 → seats released
        System.out.println("Cancel tkt-1: " + bms.cancelTicket("tkt-1"));    // true
        System.out.println("Free seats show 1: " + bms.getFreeSeatsCount(1)); // 42

        // Double-cancel → false
        System.out.println("Cancel tkt-1 again: " + bms.cancelTicket("tkt-1")); // false

        // Cancel unknown ticket → false
        System.out.println("Cancel unknown: " + bms.cancelTicket("tkt-999")); // false

        // Book more than available → empty
        List<String> tBig = bms.bookTicket("tkt-big", 1, 100);
        System.out.println("Over-book result: " + tBig);                      // []

        // Non-existent show
        System.out.println("Free seats show 99: " + bms.getFreeSeatsCount(99)); // 0
    }

    static void testJobScheduler() {
        System.out.println("\n=== JobScheduler ===");
        JobScheduler.Solution js = new JobScheduler.Solution();

        // Example 1: Multi-Capability Match + Criteria 0 (Least Unfinished) + Tie
        js.addMachine("m-10", new String[]{"image compression", "audio extraction", "video thumbnail generation"});
        js.addMachine("m-2",  new String[]{"image compression", "audio extraction"});

        String r1 = js.assignMachineToJob("job-A", new String[]{"image compression", "audio extraction"}, 0);
        System.out.println("job-A → " + r1);  // m-10 (lex smaller than m-2)

        // Example 2: Completion Updates → Criteria 1 (Most Finished)
        js.jobCompleted("job-A");  // m-10: unfinished=0, finished=1
        String r2 = js.assignMachineToJob("job-B", new String[]{"image compression"}, 1);
        System.out.println("job-B → " + r2);  // m-10 (most finished = 1)

        // Example 3: No Compatible Machine
        String r3 = js.assignMachineToJob("job-C", new String[]{"speech to text conversion"}, 0);
        System.out.println("job-C → \"" + r3 + "\"");  // ""

        // Case-insensitivity check
        js.addMachine("m-3", new String[]{"PDF Thumbnail Creator", "Plain Text Compression"});
        String r4 = js.assignMachineToJob("job-D", new String[]{"pdf thumbnail creator"}, 0);
        System.out.println("job-D (case-insensitive) → " + r4);  // m-3
    }

    static void testRateLimiter() {
        System.out.println("\n=== RateLimiter ===");
        RateLimiter.RateLimiter rl = new RateLimiter.RateLimiter();

        // Fixed-window-counter: max 2 requests every 5 seconds
        rl.addResource("login-api", "fixed-window-counter", "2,5");
        System.out.println(rl.isAllowed("login-api", 1));  // true  (count=1, window [0..4])
        System.out.println(rl.isAllowed("login-api", 2));  // true  (count=2, window [0..4])
        System.out.println(rl.isAllowed("login-api", 4));  // false (count=3 > 2, blocked)

        // Reconfigure to sliding-window-counter: max 2 requests in any 3-second window
        rl.addResource("login-api", "sliding-window-counter", "2,3");
        System.out.println(rl.isAllowed("login-api", 6));  // true  ([4..6] has {6},     count=1)
        System.out.println(rl.isAllowed("login-api", 7));  // true  ([5..7] has {6,7},   count=2)
        System.out.println(rl.isAllowed("login-api", 8));  // false ([6..8] would be {6,7,8}, count=3 > 2)
    }

    static void testShoppingCart() {
        System.out.println("\n=== ShoppingCart ===");

        // Example 1 – Normal flow
        ShoppingCart.ShoppingCart cart1 = new ShoppingCart.ShoppingCart(
                Arrays.asList("coca-cola-pack,8,12", "juice-box,5,10"));
        System.out.println(cart1.addItem("coca-cola-pack", 2));  // SUCCESS
        System.out.println(cart1.addItem("juice-box", 3));       // SUCCESS
        System.out.println(cart1.viewCart());                     // [coca-cola-pack,2, juice-box,3]
        System.out.println(cart1.checkout());                     // 31
        System.out.println(cart1.viewCart());                     // []

        // Example 2 – Out of stock
        ShoppingCart.ShoppingCart cart2 = new ShoppingCart.ShoppingCart(
                Arrays.asList("laptop,900,1"));
        System.out.println(cart2.addItem("laptop", 2));          // OUT OF STOCK
        System.out.println(cart2.viewCart());                     // []

        // Example 3 – Unavailable & empty checkout
        ShoppingCart.ShoppingCart cart3 = new ShoppingCart.ShoppingCart(
                Arrays.asList("book,50,2"));
        System.out.println(cart3.addItem("pen", 1));             // UNAVAILABLE
        System.out.println(cart3.checkout());                     // -1

        // Example 4 – Stock reservation: adding to cart doesn't let you over-commit
        ShoppingCart.ShoppingCart cart4 = new ShoppingCart.ShoppingCart(
                Arrays.asList("widget,10,3"));
        System.out.println(cart4.addItem("widget", 2));          // SUCCESS (reserved 2 of 3)
        System.out.println(cart4.addItem("widget", 2));          // OUT OF STOCK (2+2 > 3)
        System.out.println(cart4.checkout());                     // 20 (2 * 10)

        // Example 5 – Stock decrements after checkout (not reset)
        ShoppingCart.ShoppingCart cart5 = new ShoppingCart.ShoppingCart(
                Arrays.asList("coca-cola-pack,8,12"));
        cart5.addItem("coca-cola-pack", 3);
        cart5.checkout();                                         // 9 units remain
        System.out.println(cart5.addItem("coca-cola-pack", 10)); // OUT OF STOCK (only 9 left)
        System.out.println(cart5.addItem("coca-cola-pack", 9));  // SUCCESS
    }

    static void testCustomHashMap() {
        System.out.println("\n=== CustomHashMap ===");

        // Example 1 – Basic put/get/update/remove
        CustomHashMap.CustomHashMap map1 = new CustomHashMap.CustomHashMap(0.25, 0.75);
        map1.put("a", "one");
        map1.put("bb", "two");
        System.out.println(map1.get("a"));         // one
        System.out.println(map1.get("x"));         // (empty)
        map1.put("a", "ONE");                      // update
        System.out.println(map1.get("a"));         // ONE
        System.out.println(map1.remove("bb"));     // two
        System.out.println(map1.remove("bb"));     // (empty)
        System.out.println(map1.size());           // 1

        // Example 2 – Grow rehash (2 → 4 → 8) with collision tracking
        CustomHashMap.CustomHashMap map2 = new CustomHashMap.CustomHashMap(0.25, 0.75);
        map2.put("a",    "1");  // size=1, LF=0.50, buckets=2
        map2.put("bb",   "2");  // size=2, LF=1.00 > 0.75 → GROW to 4
        map2.put("abcd", "3");  // size=3, LF=0.75, buckets=4 → no rehash
        map2.put("m",    "4");  // size=4, LF=1.00 > 0.75 → GROW to 8
        map2.put("zzz",  "5");  // size=5, LF=0.63, buckets=8 → no rehash

        System.out.println(map2.bucketsCount());         // 8
        System.out.println(map2.size());                 // 5
        System.out.println(map2.getBucketKeys(0));       // [bb]
        System.out.println(map2.getBucketKeys(2));       // [a, abcd]
        System.out.println(map2.getBucketKeys(6));       // [m]
        System.out.println(map2.getBucketKeys(7));       // [zzz]
        System.out.println(map2.getBucketKeys(1));       // []
        System.out.println(map2.getBucketKeys(99));      // [] (out of bounds)

        // Example 3 – Shrink rehash on remove
        // Start: bucketsCount=8, size=5. Remove until LF < minLF (0.25)
        // After 1 remove: size=4, LF=round2(4/8)=0.50 → ok
        // After 2 removes: size=3, LF=round2(3/8)=0.38 → ok
        // After 3 removes: size=2, LF=round2(2/8)=0.25 → ok (not strictly less)
        // After 4 removes: size=1, LF=round2(1/8)=0.13 < 0.25 → SHRINK to 4
        //   round2(1/4)=0.25 → ok (not strictly less), stop
        map2.remove("a");
        map2.remove("bb");
        map2.remove("abcd");
        map2.remove("m");  // triggers shrink: 8 → 4
        System.out.println(map2.bucketsCount());        // 4
        System.out.println(map2.size());                // 1
        System.out.println(map2.get("zzz"));            // 5
    }
}
