package io.github.bridgesword.backgammon.model;

import java.util.Objects;

/**
 * The winner and standard single/gammon/backgammon classification.
 *
 * @param winner the player who bore off all 15 checkers
 * @param winType the result classification
 */
public record GameResult(Player winner, WinType winType) {
    /**
     * Creates a validated game result.
     *
     * @param winner winning player
     * @param winType result classification
     */
    public GameResult {
        Objects.requireNonNull(winner, "winner");
        Objects.requireNonNull(winType, "winType");
    }

    /**
     * Returns the number of match points awarded before use of a doubling cube.
     *
     * @return 1 for single, 2 for gammon, or 3 for backgammon
     */
    public int points() {
        return winType.points();
    }
}
