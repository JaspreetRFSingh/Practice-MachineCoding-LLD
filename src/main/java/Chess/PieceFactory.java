package Chess;

import java.util.Map;

// Flyweight factory — one stateless Piece instance per type.
// Solution depends on Piece abstraction, never on concrete classes (DIP).
class PieceFactory {

    private static final Map<Character, Piece> PIECES = Map.of(
        'K', new King(),
        'Q', new Queen(),
        'R', new Rook(),
        'B', new Bishop(),
        'H', new Knight(),
        'P', new Pawn()
    );

    static Piece get(char type) {
        Piece piece = PIECES.get(type);
        if (piece == null) throw new IllegalArgumentException("Unknown piece type: " + type);
        return piece;
    }
}
