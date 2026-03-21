package ParkingLot;

import java.util.Map;

// Flyweight factory — one stateless strategy instance per code.
// Solution depends on ParkingStrategy abstraction, not on concrete classes (DIP).
class ParkingStrategyFactory {

    private static final Map<Integer, ParkingStrategy> STRATEGIES = Map.of(
        0, new LowestIndexStrategy(),
        1, new MostFreeFloorStrategy()
    );

    static ParkingStrategy get(int strategyCode) {
        ParkingStrategy s = STRATEGIES.get(strategyCode);
        if (s == null) throw new IllegalArgumentException("Unknown parking strategy: " + strategyCode);
        return s;
    }
}
