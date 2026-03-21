package elevatorsystem;

class IdleState implements ElevatorState {
    @Override
    public void onEnter(Lift lift) {
        // No special action
    }

    @Override
    public void onExit(Lift lift) {
        // No special action
    }

    @Override
    public void tick(Lift lift) {
        // Idle lifts do nothing
    }
}
