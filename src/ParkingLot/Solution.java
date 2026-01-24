package ParkingLot;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Solution {

    // spotId ("f-r-c") → vehicle type (2 or 4); immutable after init
    private final Map<String, Integer> spotType = new HashMap<>();

    // Sort spots by (floor, row, col) — natural priority order for both strategies
    private static final Comparator<int[]> SPOT_ORDER =
        (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                : a[1] != b[1] ? Integer.compare(a[1], b[1])
                :                Integer.compare(a[2], b[2]);

    // Strategy-0: pollFirst() gives the globally lowest-index free spot
    private final TreeSet<int[]> globalFree2 = new TreeSet<>(SPOT_ORDER);
    private final TreeSet<int[]> globalFree4 = new TreeSet<>(SPOT_ORDER);

    // Strategy-1: pollFirst() gives the lowest spot on a chosen floor
    private TreeSet<int[]>[] freePerFloor2;
    private TreeSet<int[]>[] freePerFloor4;

    // Strategy-1 floor selection: O(F) scan, F ≤ 5; also serves getFreeSpotsCount()
    private AtomicInteger[] freeCount2;
    private AtomicInteger[] freeCount4;

    // One lock per vehicle type — 2-wheeler and 4-wheeler threads never block each other
    private final ReentrantLock lock2 = new ReentrantLock();
    private final ReentrantLock lock4 = new ReentrantLock();

    // spotId → [vehicleNumber, ticketId]
    private final ConcurrentHashMap<String, String[]> occupiedSpots = new ConcurrentHashMap<>();
    // vehicleNumber / ticketId → current active spotId
    private final ConcurrentHashMap<String, String> vehicleToSpot = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> ticketToSpot  = new ConcurrentHashMap<>();

    private int numFloors;

    @SuppressWarnings("unchecked")
    public void init(int[][][] parking) {
        this.numFloors = parking.length;

        freePerFloor2 = new TreeSet[numFloors];
        freePerFloor4 = new TreeSet[numFloors];
        freeCount2    = new AtomicInteger[numFloors];
        freeCount4    = new AtomicInteger[numFloors];

        for (int f = 0; f < numFloors; f++) {
            freePerFloor2[f] = new TreeSet<>(SPOT_ORDER);
            freePerFloor4[f] = new TreeSet<>(SPOT_ORDER);
            freeCount2[f]    = new AtomicInteger(0);
            freeCount4[f]    = new AtomicInteger(0);

            for (int r = 0; r < parking[f].length; r++) {
                for (int c = 0; c < parking[f][r].length; c++) {
                    int type = parking[f][r][c];
                    if (type == 0) continue; // inactive spot

                    String id   = spotId(f, r, c);
                    int[]  spot = {f, r, c};
                    spotType.put(id, type);

                    if (type == 2) {
                        globalFree2.add(spot);
                        freePerFloor2[f].add(spot);
                        freeCount2[f].incrementAndGet();
                    } else {
                        globalFree4.add(spot);
                        freePerFloor4[f].add(spot);
                        freeCount4[f].incrementAndGet();
                    }
                }
            }
        }
    }

    public String park(int vehicleType, String vehicleNumber, String ticketId, int parkingStrategy) {
        ReentrantLock    lock         = lock(vehicleType);
        TreeSet<int[]>   globalFree   = globalFree(vehicleType);
        TreeSet<int[]>[] freePerFloor = freePerFloor(vehicleType);
        AtomicInteger[]  freeCount    = freeCount(vehicleType);

        lock.lock();
        try {
            // Delegate spot selection to the chosen strategy (OCP: new strategy = new class only)
            int[] spot = ParkingStrategyFactory.get(parkingStrategy)
                                               .allocate(globalFree, freePerFloor, freeCount);
            if (spot == null) return "";

            String id = spotId(spot[0], spot[1], spot[2]);
            occupiedSpots.put(id, new String[]{vehicleNumber, ticketId});
            vehicleToSpot.put(vehicleNumber, id);
            ticketToSpot.put(ticketId, id);
            return id;

        } finally {
            lock.unlock();
        }
    }

    public boolean removeVehicle(String spotId) {
        // ConcurrentHashMap.remove is atomic — concurrent unparks of the same spot are safe
        String[] record = occupiedSpots.remove(spotId);
        if (record == null) return false;

        vehicleToSpot.remove(record[0]);
        ticketToSpot.remove(record[1]);

        int   type = spotType.get(spotId);
        int[] spot = parseSpotId(spotId);

        lock(type).lock();
        try {
            globalFree(type).add(spot);
            freePerFloor(type)[spot[0]].add(spot);
            freeCount(type)[spot[0]].incrementAndGet();
        } finally {
            lock(type).unlock();
        }
        return true;
    }

    public String searchVehicle(String query) {
        String id = vehicleToSpot.get(query);
        if (id == null) id = ticketToSpot.get(query);
        return id != null ? id : "";
    }

    // AtomicInteger.get() is a volatile read — no lock needed for a count query
    public int getFreeSpotsCount(int floor, int vehicleType) {
        return (vehicleType == 2 ? freeCount2 : freeCount4)[floor].get();
    }

    private static String spotId(int f, int r, int c) { return f + "-" + r + "-" + c; }

    private static int[] parseSpotId(String id) {
        String[] p = id.split("-");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }

    private ReentrantLock    lock(int type)         { return type == 2 ? lock2 : lock4; }
    private TreeSet<int[]>   globalFree(int type)   { return type == 2 ? globalFree2 : globalFree4; }

    @SuppressWarnings("unchecked")
    private TreeSet<int[]>[] freePerFloor(int type) { return type == 2 ? freePerFloor2 : freePerFloor4; }
    private AtomicInteger[]  freeCount(int type)    { return type == 2 ? freeCount2 : freeCount4; }
}
