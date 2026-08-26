package io.github.bridgesword.backgammon.model;

import java.util.Objects;

/**
 * One checker movement made with one die.
 *
 * @param player the moving player
 * @param from the source point or {@link Point#BAR}
 * @param to the destination point or {@link Point#OFF}
 * @param die the die consumed by the move
 */
public record Move(Player player, Point from, Point to, int die) {
    /**
     * Validates the structural parts of a move. Board legality is validated by
     * the rules engine.
     *
     * @param player the moving player
     * @param from source location
     * @param to destination location
     * @param die consumed die
     */
    public Move {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.kind() == Point.Kind.OFF) {
            throw new IllegalArgumentException("A move cannot start off the board");
        }
        if (to.kind() == Point.Kind.BAR) {
            throw new IllegalArgumentException("A move cannot end on the bar");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("Move source and destination must differ");
        }
        if (die < 1 || die > 6) {
            throw new IllegalArgumentException("Die must be between 1 and 6");
        }
    }

    @Override
    public String toString() {
        return player.displayName() + ": " + from + " -> " + to + " (" + die + ")";
    }
}
