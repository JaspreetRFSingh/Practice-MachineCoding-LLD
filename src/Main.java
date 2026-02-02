import HitCounter.HitCounter;

public class Main {
    public static void main(String[] args) {
        testHitCounter();
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
}
