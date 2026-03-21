package ParkingLot;

import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

// Strategy interface — each implementation picks and claims a spot from the available sets.
// Returns the chosen spot [floor, row, col], or null if no spot is available.
// Runs under the vehicle-type lock in Solution — implementations need not be thread-safe themselves.
interface ParkingStrategy {
    int[] allocate(TreeSet<int[]> globalFree,
                   TreeSet<int[]>[] freePerFloor,
                   AtomicInteger[] freeCount);
}
