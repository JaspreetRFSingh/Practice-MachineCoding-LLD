package ParkingLot;

import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

// Strategy 1: pick the floor with the most free spots (tie → lowest floor index),
// then take the lowest-index spot on that floor.
class MostFreeFloorStrategy implements ParkingStrategy {

    @Override
    public int[] allocate(TreeSet<int[]> globalFree,
                          TreeSet<int[]>[] freePerFloor,
                          AtomicInteger[] freeCount) {
        int bestFloor = -1, maxFree = 0;
        for (int f = 0; f < freeCount.length; f++) {
            int count = freeCount[f].get();
            if (count > maxFree) { maxFree = count; bestFloor = f; }
        }
        if (bestFloor == -1) return null;

        int[] spot = freePerFloor[bestFloor].pollFirst();
        globalFree.remove(spot);
        freeCount[bestFloor].decrementAndGet();
        return spot;
    }
}
