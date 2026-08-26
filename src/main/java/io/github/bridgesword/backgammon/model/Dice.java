package io.github.bridgesword.backgammon.model;

import java.util.List;

/**
 * A two-die roll. Doubles expand to four playable die values.
 *
 * @param first the first die
 * @param second the second die
 */
public record Dice(int first, int second) {
    /**
     * Validates both dice.
     *
     * @param first first value
     * @param second second value
     */
    public Dice {
        validate(first);
        validate(second);
    }

    /**
     * Returns the dice as the values available during a turn. A double produces
     * four equal values.
     *
     * @return an immutable list containing two or four values
     */
    public List<Integer> expanded() {
        return isDouble()
                ? List.of(first, first, first, first)
                : List.of(first, second);
    }

    /**
     * Returns whether both dice show the same value.
     *
     * @return {@code true} for doubles
     */
    public boolean isDouble() {
        return first == second;
    }

    private static void validate(int die) {
        if (die < 1 || die > 6) {
            throw new IllegalArgumentException("Die must be between 1 and 6");
        }
    }
}
