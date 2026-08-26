package io.github.bridgesword.backgammon.model;

import java.util.List;
import java.util.Objects;

/**
 * A complete legal turn together with its resulting board.
 *
 * @param moves ordered checker moves; the list is defensively copied
 * @param resultingBoard immutable board after all moves
 */
public record MoveSequence(List<Move> moves, Board resultingBoard) {
    /**
     * Creates an immutable move sequence.
     *
     * @param moves ordered moves
     * @param resultingBoard board after those moves
     */
    public MoveSequence {
        moves = List.copyOf(Objects.requireNonNull(moves, "moves"));
        resultingBoard = Objects.requireNonNull(resultingBoard, "resultingBoard");
    }

    /**
     * Returns whether the turn contains no playable move.
     *
     * @return {@code true} for an empty sequence
     */
    public boolean isEmpty() {
        return moves.isEmpty();
    }

    /**
     * Returns the first move in the turn.
     *
     * @return the first move
     * @throws IllegalStateException if the sequence is empty
     */
    public Move firstMove() {
        if (moves.isEmpty()) {
            throw new IllegalStateException("An empty sequence has no first move");
        }
        return moves.get(0);
    }
}
