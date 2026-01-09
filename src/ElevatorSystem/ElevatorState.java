package elevatorsystem;

interface ElevatorState {
    void onEnter(Lift lift);
    void onExit(Lift lift);
    void tick(Lift lift);
}
