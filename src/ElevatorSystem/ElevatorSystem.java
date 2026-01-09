package elevatorsystem;

import java.util.*;

public class ElevatorSystem {
    private int floors;
    private List<Lift> lifts;
    private Helper11 helper;

    public void init(int floors, int lifts, int liftsCapacity, Helper11 helper) {
        this.floors = floors;
        this.helper = helper;
        this.lifts = new ArrayList<>(lifts);
        for (int i = 0; i < lifts; i++) {
            this.lifts.add(new Lift(liftsCapacity));
        }
    }

    public int requestLift(int startFloor, char direction) {
        int bestLift = -1;
        int bestTime = Integer.MAX_VALUE;
        for (int i = 0; i < lifts.size(); i++) {
            Lift lift = lifts.get(i);
            if (isEligible(lift, startFloor, direction)) {
                int time = computeTime(lift, startFloor, direction);
                if (time < bestTime) {
                    bestTime = time;
                    bestLift = i;
                }
            }
        }
        if (bestLift != -1) {
            Lift lift = lifts.get(bestLift);
            lift.addExternalRequest(startFloor, direction);
        }
        return bestLift;
    }

    public void pressFloorButtonInLift(int liftIndex, int floor) {
        Lift lift = lifts.get(liftIndex);
        lift.addInternalRequest(floor);
    }

    public String getLiftState(int liftIndex) {
        Lift lift = lifts.get(liftIndex);
        return lift.currentFloor + "-" + lift.getDirection() + "-" + lift.peopleCount;
    }

    public void tick() {
        for (Lift lift : lifts) {
            lift.tick();
        }
    }

    // ---------- Eligibility and time calculation ----------
    private boolean isEligible(Lift lift, int startFloor, char direction) {
        char effectiveDir = lift.getDirection();
        // If idle but has pending external at current floor, use that direction
        if (effectiveDir == 'I' && lift.pendingExternal.containsKey(lift.currentFloor)) {
            Set<Character> dirSet = lift.pendingExternal.get(lift.currentFloor);
            if (!dirSet.isEmpty()) {
                effectiveDir = dirSet.iterator().next();
            }
        }
        // Truly idle
        if (effectiveDir == 'I') return true;
        // Already passed?
        if (effectiveDir == 'U' && startFloor < lift.currentFloor) return false;
        if (effectiveDir == 'D' && startFloor > lift.currentFloor) return false;

        // Determine stops in the effective direction (excluding current floor)
        List<Integer> stopsInDir = new ArrayList<>();
        if (effectiveDir == 'U') {
            for (int f : lift.pendingExternal.keySet()) if (f > lift.currentFloor) stopsInDir.add(f);
            for (int f : lift.pendingInternal.keySet()) if (f > lift.currentFloor) stopsInDir.add(f);
        } else { // 'D'
            for (int f : lift.pendingExternal.keySet()) if (f < lift.currentFloor) stopsInDir.add(f);
            for (int f : lift.pendingInternal.keySet()) if (f < lift.currentFloor) stopsInDir.add(f);
        }

        // Are there internal stops in the current direction?
        boolean internalInDir = false;
        for (int f : stopsInDir) {
            if (lift.pendingInternal.containsKey(f)) {
                internalInDir = true;
                break;
            }
        }
        // Also, a pending external at current floor creates an implicit internal stop
        if (lift.pendingExternal.containsKey(lift.currentFloor) &&
                lift.pendingExternal.get(lift.currentFloor).contains(effectiveDir)) {
            internalInDir = true;
        }

        // No stops in current direction -> lift will reverse, so any request is eligible
        if (stopsInDir.isEmpty()) return true;

        // Case with internal stops (including implicit)
        if (internalInDir) {
            // Opposite direction not allowed
            if (direction != effectiveDir) return false;
            // If there are explicit internal stops, check bound
            boolean explicitInternal = false;
            for (int f : stopsInDir) {
                if (lift.pendingInternal.containsKey(f)) {
                    explicitInternal = true;
                    break;
                }
            }
            if (explicitInternal) {
                if (effectiveDir == 'U') {
                    int maxStop = Collections.max(stopsInDir);
                    return startFloor <= maxStop;
                } else {
                    int minStop = Collections.min(stopsInDir);
                    return startFloor >= minStop;
                }
            } else {
                // Only implicit internal stops -> no bound
                return true;
            }
        }
        // Only external stops in current direction
        else {
            // Determine the direction(s) of these external stops
            Set<Character> dirs = new HashSet<>();
            for (int f : stopsInDir) {
                if (lift.pendingExternal.containsKey(f)) {
                    dirs.addAll(lift.pendingExternal.get(f));
                }
            }
            if (dirs.size() > 1) {
                // Mixed directions → treat as internal case
                if (direction != effectiveDir) return false;
                if (effectiveDir == 'U') {
                    int maxStop = Collections.max(stopsInDir);
                    return startFloor <= maxStop;
                } else {
                    int minStop = Collections.min(stopsInDir);
                    return startFloor >= minStop;
                }
            } else {
                char eventualDir = dirs.iterator().next();
                if (direction != eventualDir) return false;
                if (effectiveDir == 'U') {
                    int maxStop = Collections.max(stopsInDir);
                    return startFloor <= maxStop;
                } else {
                    int minStop = Collections.min(stopsInDir);
                    return startFloor >= minStop;
                }
            }
        }
    }

    private int computeTime(Lift lift, int startFloor, char direction) {
        // Immediate arrival
        if (startFloor == lift.currentFloor) return 0;
        // Build set of all pending stop floors (including the new request)
        Set<Integer> stops = new HashSet<>();
        stops.addAll(lift.pendingExternal.keySet());
        stops.addAll(lift.pendingInternal.keySet());
        stops.add(startFloor);
        int floor = lift.currentFloor;
        char dir = lift.getDirection();
        // Process any stop at the current floor
        if (stops.contains(floor)) {
            stops.remove(floor);
            // Update direction after processing this stop
            if (dir == 'U') {
                boolean anyUp = false;
                for (int f : stops) if (f > floor) anyUp = true;
                if (!anyUp) {
                    boolean anyDown = false;
                    for (int f : stops) if (f < floor) anyDown = true;
                    dir = anyDown ? 'D' : 'I';
                }
            } else if (dir == 'D') {
                boolean anyDown = false;
                for (int f : stops) if (f < floor) anyDown = true;
                if (!anyDown) {
                    boolean anyUp = false;
                    for (int f : stops) if (f > floor) anyUp = true;
                    dir = anyUp ? 'U' : 'I';
                }
            }
        }
        // If now idle, just go directly
        if (dir == 'I') return Math.abs(startFloor - floor);
        // Simulate movement floor by floor
        int time = 0;
        while (floor != startFloor) {
            if (dir == 'U') {
                floor++; time++;
                if (stops.contains(floor)) {
                    stops.remove(floor);
                    boolean anyUp = false;
                    for (int f : stops) if (f > floor) anyUp = true;
                    if (!anyUp) {
                        boolean anyDown = false;
                        for (int f : stops) if (f < floor) anyDown = true;
                        dir = anyDown ? 'D' : 'I';
                    }
                }
            } else { // dir == 'D'
                floor--; time++;
                if (stops.contains(floor)) {
                    stops.remove(floor);
                    boolean anyDown = false;
                    for (int f : stops) if (f < floor) anyDown = true;
                    if (!anyDown) {
                        boolean anyUp = false;
                        for (int f : stops) if (f > floor) anyUp = true;
                        dir = anyUp ? 'U' : 'I';
                    }
                }
            }
        }
        return time;
    }
}
