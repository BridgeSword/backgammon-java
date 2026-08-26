package io.github.bridgesword.backgammon.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable backgammon board containing all 30 checkers across the 24
 * points, both bars, and both borne-off trays.
 *
 * <p>Public point numbers are 1 through 24. White moves toward lower numbers;
 * Black moves toward higher numbers. All array-returning methods return
 * defensive copies.</p>
 */
public final class Board {
    /** Number of numbered points on a backgammon board. */
    public static final int POINT_COUNT = 24;
    /** Number of checkers owned by each player. */
    public static final int CHECKERS_PER_PLAYER = 15;

    private final int[] whitePoints;
    private final int[] blackPoints;
    private final int whiteBar;
    private final int blackBar;
    private final int whiteOff;
    private final int blackOff;

    private Board(Builder builder) {
        whitePoints = builder.whitePoints.clone();
        blackPoints = builder.blackPoints.clone();
        whiteBar = builder.whiteBar;
        blackBar = builder.blackBar;
        whiteOff = builder.whiteOff;
        blackOff = builder.blackOff;
        validate();
    }

    /**
     * Returns the standard opening position.
     *
     * <p>White starts with checkers on points 24/13/8/6 and Black on
     * points 1/12/17/19, with counts 2/5/3/5 respectively.</p>
     *
     * @return the standard immutable board
     */
    public static Board initial() {
        return builder()
                .point(24, Player.WHITE, 2)
                .point(13, Player.WHITE, 5)
                .point(8, Player.WHITE, 3)
                .point(6, Player.WHITE, 5)
                .point(1, Player.BLACK, 2)
                .point(12, Player.BLACK, 5)
                .point(17, Player.BLACK, 3)
                .point(19, Player.BLACK, 5)
                .build();
    }

    /**
     * Starts an empty builder. The completed board must account for exactly 15
     * checkers per player, including bar and off-board checkers.
     *
     * @return a mutable builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts a builder initialized from an existing board.
     *
     * @param board board to copy
     * @return a mutable builder containing the same position
     */
    public static Builder builder(Board board) {
        return new Builder(Objects.requireNonNull(board, "board"));
    }

    /**
     * Returns the number of a player's checkers on a numbered point.
     *
     * @param pointNumber a point from 1 through 24
     * @param player checker owner
     * @return checker count, from 0 through 15
     */
    public int countAt(int pointNumber, Player player) {
        requirePoint(pointNumber);
        Objects.requireNonNull(player, "player");
        return pointsInternal(player)[pointNumber - 1];
    }

    /**
     * Returns the number of a player's checkers at any location.
     *
     * @param point board, bar, or off location
     * @param player checker owner
     * @return the checker count
     */
    public int countAt(Point point, Player player) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(player, "player");
        return switch (point.kind()) {
            case BOARD -> countAt(point.number(), player);
            case BAR -> bar(player);
            case OFF -> borneOff(player);
        };
    }

    /**
     * Finds the owner of a numbered point.
     *
     * @param pointNumber a point from 1 through 24
     * @return the owner, or empty if the point is empty
     */
    public Optional<Player> ownerAt(int pointNumber) {
        requirePoint(pointNumber);
        if (whitePoints[pointNumber - 1] > 0) {
            return Optional.of(Player.WHITE);
        }
        if (blackPoints[pointNumber - 1] > 0) {
            return Optional.of(Player.BLACK);
        }
        return Optional.empty();
    }

    /**
     * Returns a UI-friendly signed point array. White checkers are positive and
     * Black checkers negative; index zero represents point 1.
     *
     * @return a defensive 24-element copy
     */
    public int[] signedPoints() {
        int[] result = new int[POINT_COUNT];
        for (int i = 0; i < POINT_COUNT; i++) {
            result[i] = whitePoints[i] - blackPoints[i];
        }
        return result;
    }

    /**
     * Returns all point counts for one player. Index zero represents point 1.
     *
     * @param player checker owner
     * @return a defensive 24-element copy
     */
    public int[] points(Player player) {
        Objects.requireNonNull(player, "player");
        return pointsInternal(player).clone();
    }

    /**
     * Returns the number of checkers on a player's bar.
     *
     * @param player checker owner
     * @return bar count
     */
    public int bar(Player player) {
        Objects.requireNonNull(player, "player");
        return player == Player.WHITE ? whiteBar : blackBar;
    }

    /**
     * Returns the number of checkers a player has borne off.
     *
     * @param player checker owner
     * @return borne-off count
     */
    public int borneOff(Player player) {
        Objects.requireNonNull(player, "player");
        return player == Player.WHITE ? whiteOff : blackOff;
    }

    /**
     * Tests whether all of a player's checkers not already off are in the home
     * board and none are on the bar.
     *
     * @param player checker owner
     * @return whether bearing off is currently permitted
     */
    public boolean allInHome(Player player) {
        Objects.requireNonNull(player, "player");
        if (bar(player) != 0) {
            return false;
        }
        int[] counts = pointsInternal(player);
        for (int point = 1; point <= POINT_COUNT; point++) {
            if (!player.isHomePoint(point) && counts[point - 1] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the conventional pip count, including 25 pips per checker on the
     * bar.
     *
     * @param player checker owner
     * @return total pips remaining to bear off
     */
    public int pipCount(Player player) {
        Objects.requireNonNull(player, "player");
        int result = 25 * bar(player);
        int[] counts = pointsInternal(player);
        for (int point = 1; point <= POINT_COUNT; point++) {
            int distance = player == Player.WHITE ? point : 25 - point;
            result += counts[point - 1] * distance;
        }
        return result;
    }

    private int[] pointsInternal(Player player) {
        return player == Player.WHITE ? whitePoints : blackPoints;
    }

    private void validate() {
        int whiteTotal = whiteBar + whiteOff;
        int blackTotal = blackBar + blackOff;
        if (whiteBar < 0 || blackBar < 0 || whiteOff < 0 || blackOff < 0) {
            throw new IllegalArgumentException("Checker counts cannot be negative");
        }
        for (int i = 0; i < POINT_COUNT; i++) {
            if (whitePoints[i] < 0 || blackPoints[i] < 0) {
                throw new IllegalArgumentException("Checker counts cannot be negative");
            }
            if (whitePoints[i] > 0 && blackPoints[i] > 0) {
                throw new IllegalArgumentException("Both players cannot occupy point " + (i + 1));
            }
            whiteTotal += whitePoints[i];
            blackTotal += blackPoints[i];
        }
        if (whiteTotal != CHECKERS_PER_PLAYER || blackTotal != CHECKERS_PER_PLAYER) {
            throw new IllegalArgumentException(
                    "A board must contain exactly 15 checkers per player (White="
                            + whiteTotal + ", Black=" + blackTotal + ")");
        }
    }

    private static void requirePoint(int pointNumber) {
        if (pointNumber < 1 || pointNumber > POINT_COUNT) {
            throw new IllegalArgumentException("Point must be between 1 and 24: " + pointNumber);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Board other)) {
            return false;
        }
        return whiteBar == other.whiteBar
                && blackBar == other.blackBar
                && whiteOff == other.whiteOff
                && blackOff == other.blackOff
                && Arrays.equals(whitePoints, other.whitePoints)
                && Arrays.equals(blackPoints, other.blackPoints);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(whitePoints);
        result = 31 * result + Arrays.hashCode(blackPoints);
        result = 31 * result + whiteBar;
        result = 31 * result + blackBar;
        result = 31 * result + whiteOff;
        result = 31 * result + blackOff;
        return result;
    }

    @Override
    public String toString() {
        return "Board{points=" + Arrays.toString(signedPoints())
                + ", whiteBar=" + whiteBar
                + ", blackBar=" + blackBar
                + ", whiteOff=" + whiteOff
                + ", blackOff=" + blackOff + '}';
    }

    /** Mutable builder used to assemble a validated immutable board. */
    public static final class Builder {
        private final int[] whitePoints = new int[POINT_COUNT];
        private final int[] blackPoints = new int[POINT_COUNT];
        private int whiteBar;
        private int blackBar;
        private int whiteOff;
        private int blackOff;

        private Builder() {
        }

        private Builder(Board board) {
            System.arraycopy(board.whitePoints, 0, whitePoints, 0, POINT_COUNT);
            System.arraycopy(board.blackPoints, 0, blackPoints, 0, POINT_COUNT);
            whiteBar = board.whiteBar;
            blackBar = board.blackBar;
            whiteOff = board.whiteOff;
            blackOff = board.blackOff;
        }

        /**
         * Replaces the contents of a numbered point. Setting a positive count
         * clears any opposing checkers from that point.
         *
         * @param pointNumber point from 1 through 24
         * @param player checker owner
         * @param count number of checkers
         * @return this builder
         */
        public Builder point(int pointNumber, Player player, int count) {
            requirePoint(pointNumber);
            Objects.requireNonNull(player, "player");
            requireCount(count);
            int index = pointNumber - 1;
            if (player == Player.WHITE) {
                whitePoints[index] = count;
                if (count > 0) {
                    blackPoints[index] = 0;
                }
            } else {
                blackPoints[index] = count;
                if (count > 0) {
                    whitePoints[index] = 0;
                }
            }
            return this;
        }

        /**
         * Empties a numbered point.
         *
         * @param pointNumber point from 1 through 24
         * @return this builder
         */
        public Builder clearPoint(int pointNumber) {
            requirePoint(pointNumber);
            whitePoints[pointNumber - 1] = 0;
            blackPoints[pointNumber - 1] = 0;
            return this;
        }

        /**
         * Sets a player's bar count.
         *
         * @param player checker owner
         * @param count checker count
         * @return this builder
         */
        public Builder bar(Player player, int count) {
            Objects.requireNonNull(player, "player");
            requireCount(count);
            if (player == Player.WHITE) {
                whiteBar = count;
            } else {
                blackBar = count;
            }
            return this;
        }

        /**
         * Sets a player's borne-off count.
         *
         * @param player checker owner
         * @param count checker count
         * @return this builder
         */
        public Builder borneOff(Player player, int count) {
            Objects.requireNonNull(player, "player");
            requireCount(count);
            if (player == Player.WHITE) {
                whiteOff = count;
            } else {
                blackOff = count;
            }
            return this;
        }

        /**
         * Validates and builds an immutable board.
         *
         * @return the completed board
         */
        public Board build() {
            return new Board(this);
        }

        private static void requireCount(int count) {
            if (count < 0 || count > CHECKERS_PER_PLAYER) {
                throw new IllegalArgumentException("Checker count must be between 0 and 15");
            }
        }
    }
}
