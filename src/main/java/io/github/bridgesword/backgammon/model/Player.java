package io.github.bridgesword.backgammon.model;

/**
 * One of the two backgammon players.
 *
 * <p>Points are numbered 1 through 24. White moves from point 24 toward point
 * 1; Black moves from point 1 toward point 24.</p>
 */
public enum Player {
    /** Player moving from point 24 toward point 1. */
    WHITE(-1, "White"),
    /** Player moving from point 1 toward point 24. */
    BLACK(1, "Black");

    private final int direction;
    private final String displayName;

    Player(int direction, String displayName) {
        this.direction = direction;
        this.displayName = displayName;
    }

    /**
     * Returns {@code -1} for White and {@code +1} for Black.
     *
     * @return the change in point number for one pip of movement
     */
    public int direction() {
        return direction;
    }

    /**
     * Returns the opposing player.
     *
     * @return the other player
     */
    public Player opponent() {
        return this == WHITE ? BLACK : WHITE;
    }

    /**
     * Tests whether a point is in this player's home board.
     *
     * @param pointNumber a board point from 1 through 24
     * @return {@code true} when the point is in the player's home board
     */
    public boolean isHomePoint(int pointNumber) {
        return this == WHITE
                ? pointNumber >= 1 && pointNumber <= 6
                : pointNumber >= 19 && pointNumber <= 24;
    }

    /**
     * Maps a die to the point entered from the bar.
     *
     * @param die a die value from 1 through 6
     * @return the corresponding entry point
     */
    public int entryPoint(int die) {
        if (die < 1 || die > 6) {
            throw new IllegalArgumentException("Die must be between 1 and 6");
        }
        return this == WHITE ? 25 - die : die;
    }

    /**
     * Returns the die value needed to bear a checker off exactly.
     *
     * @param pointNumber a point in this player's home board
     * @return the exact bearing-off die
     */
    public int bearingOffDistance(int pointNumber) {
        if (!isHomePoint(pointNumber)) {
            throw new IllegalArgumentException("Point is outside the player's home board: " + pointNumber);
        }
        return this == WHITE ? pointNumber : 25 - pointNumber;
    }

    /**
     * Returns a human-friendly player name.
     *
     * @return {@code "White"} or {@code "Black"}
     */
    public String displayName() {
        return displayName;
    }
}
