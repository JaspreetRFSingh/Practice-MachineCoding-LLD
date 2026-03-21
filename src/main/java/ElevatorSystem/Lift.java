package elevatorsystem;

import java.util.*;

class Lift {
    int currentFloor;
    int peopleCount;
    int capacity;
    Map<Integer, Set<Character>> pendingExternal;  // floor -> directions
    Map<Integer, Integer> pendingInternal;         // floor -> people count
    ElevatorState currentState;

    Lift(int capacity) {
        this.currentFloor = 0;
        this.peopleCount = 0;
        this.capacity = capacity;
        this.pendingExternal = new HashMap<>();
        this.pendingInternal = new HashMap<>();
        this.currentState = new IdleState();
        this.currentState.onEnter(this);
    }

    // Called when an external request is assigned to this lift
    void addExternalRequest(int floor, char direction) {
        pendingExternal.computeIfAbsent(floor, k -> new HashSet<>()).add(direction);
        transitionToAppropriateState();
    }

    // Called when a passenger inside the lift presses a floor button
    void addInternalRequest(int floor) {
        peopleCount++;
        pendingInternal.put(floor, pendingInternal.getOrDefault(floor, 0) + 1);
        transitionToAppropriateState();
    }

    // Determine the correct state based on pending stops and current floor
    void transitionToAppropriateState() {
        boolean hasUp = false, hasDown = false;
        for (int f : pendingExternal.keySet()) {
            if (f > currentFloor) hasUp = true;
            else if (f < currentFloor) hasDown = true;
        }
        for (int f : pendingInternal.keySet()) {
            if (f > currentFloor) hasUp = true;
            else if (f < currentFloor) hasDown = true;
        }

        if (!hasUp && !hasDown) {
            setState(new IdleState());
        } else if (hasUp && !hasDown) {
            setState(new MovingUpState());
        } else if (hasDown && !hasUp) {
            setState(new MovingDownState());
        } else {
            // Both directions exist – keep current state if already moving,
            // otherwise default to moving up (spec says lifts start idle at floor 0)
            if (currentState instanceof IdleState) {
                setState(new MovingUpState());
            }
            // else stay in current state (MovingUp or MovingDown)
        }
    }

    void setState(ElevatorState newState) {
        if (currentState != null) currentState.onExit(this);
        currentState = newState;
        currentState.onEnter(this);
    }

    void tick() {
        currentState.tick(this);
    }

    // Helper to get the current direction (for eligibility / time calculation)
    char getDirection() {
        if (currentState instanceof MovingUpState) return 'U';
        if (currentState instanceof MovingDownState) return 'D';
        return 'I';
    }
}
