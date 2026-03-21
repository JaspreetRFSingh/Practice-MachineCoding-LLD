package elevatorsystem;

class MovingUpState implements ElevatorState {
    @Override
    public void onEnter(Lift lift) {
        // Could set a flag or log
    }

    @Override
    public void onExit(Lift lift) {
        // No special action
    }

    @Override
    public void tick(Lift lift) {
        lift.currentFloor++;
        int floor = lift.currentFloor;

        // Process external pickups on this floor
        if (lift.pendingExternal.containsKey(floor)) {
            lift.pendingExternal.remove(floor);
        }
        // Process internal dropoffs on this floor
        if (lift.pendingInternal.containsKey(floor)) {
            int count = lift.pendingInternal.get(floor);
            lift.peopleCount -= count;
            lift.pendingInternal.remove(floor);
        }

        // Check if there are still stops above
        boolean hasUp = false;
        for (int f : lift.pendingExternal.keySet()) {
            if (f > floor) { hasUp = true; break; }
        }
        if (!hasUp) {
            for (int f : lift.pendingInternal.keySet()) {
                if (f > floor) { hasUp = true; break; }
            }
        }

        if (!hasUp) {
            // No more up stops → check for down stops
            boolean hasDown = false;
            for (int f : lift.pendingExternal.keySet()) {
                if (f < floor) { hasDown = true; break; }
            }
            if (!hasDown) {
                for (int f : lift.pendingInternal.keySet()) {
                    if (f < floor) { hasDown = true; break; }
                }
            }
            if (hasDown) {
                lift.setState(new MovingDownState());
            } else {
                lift.setState(new IdleState());
            }
        }
    }
}
