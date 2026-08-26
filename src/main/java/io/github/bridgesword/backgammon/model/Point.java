package io.github.bridgesword.backgammon.model;

import java.util.Objects;

/**
 * A checker location: one of the 24 numbered board points, the bar, or off the
 * board. Instances are immutable and suitable for use as map keys.
 *
 * @param kind the location category
 * @param number a board point number, or zero for {@link Kind#BAR} and {@link Kind#OFF}
 */
public record Point(Kind kind, int number) implements Comparable<Point> {
    /** The shared bar location. */
    public static final Point BAR = new Point(Kind.BAR, 0);
    /** The shared borne-off location. */
    public static final Point OFF = new Point(Kind.OFF, 0);

    /** Categories of checker location. */
    public enum Kind {
        /** A numbered point from 1 through 24. */
        BOARD,
        /** The central bar holding hit checkers. */
        BAR,
        /** The tray holding borne-off checkers. */
        OFF
    }

    /**
     * Validates a point value.
     *
     * @param kind the location category
     * @param number a board point number, or zero for bar/off
     */
    public Point {
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.BOARD && (number < 1 || number > 24)) {
            throw new IllegalArgumentException("Board point must be between 1 and 24");
        }
        if (kind != Kind.BOARD && number != 0) {
            throw new IllegalArgumentException("Bar and off locations use point number 0");
        }
    }

    /**
     * Creates a numbered board point.
     *
     * @param number a point number from 1 through 24
     * @return the immutable point
     */
    public static Point board(int number) {
        return new Point(Kind.BOARD, number);
    }

    /**
     * Returns whether this is a numbered board point.
     *
     * @return {@code true} for a point from 1 through 24
     */
    public boolean isBoard() {
        return kind == Kind.BOARD;
    }

    /**
     * Returns a concise label suitable for a board UI.
     *
     * @return the number, {@code "Bar"}, or {@code "Off"}
     */
    public String displayName() {
        return switch (kind) {
            case BOARD -> Integer.toString(number);
            case BAR -> "Bar";
            case OFF -> "Off";
        };
    }

    @Override
    public int compareTo(Point other) {
        int kindComparison = kind.compareTo(other.kind);
        return kindComparison != 0 ? kindComparison : Integer.compare(number, other.number);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
