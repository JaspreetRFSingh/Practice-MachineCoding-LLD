package ParkingLot;

import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

// Strategy 0: always pick the globally lowest (floor → row → col) free spot.
class LowestIndexStrategy implements ParkingStrategy {

    @Override
    public int[] allocate(TreeSet<int[]> globalFree,
                          TreeSet<int[]>[] freePerFloor,
                          AtomicInteger[] freeCount) {
        int[] spot = globalFree.pollFirst();
        if (spot == null) return null;
        freePerFloor[spot[0]].remove(spot);
        freeCount[spot[0]].decrementAndGet();
        return spot;
    }
}
