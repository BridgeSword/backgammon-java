package io.github.bridgesword.backgammon.model;

/** Standard backgammon win classifications and their match-point values. */
public enum WinType {
    /** The loser bore off at least one checker. */
    SINGLE(1),
    /** The loser bore off none, but is neither on the bar nor in the winner's home board. */
    GAMMON(2),
    /** The loser bore off none and remains on the bar or in the winner's home board. */
    BACKGAMMON(3);

    private final int points;

    WinType(int points) {
        this.points = points;
    }

    /**
     * Returns the score multiplier for this win type.
     *
     * @return 1, 2, or 3 points
     */
    public int points() {
        return points;
    }
}
