package io.github.bridgesword.backgammon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DiceTest {
    @Test
    void ordinaryRollPreservesBothDiceInOrder() {
        Dice dice = new Dice(2, 5);

        assertFalse(dice.isDouble());
        assertEquals(List.of(2, 5), dice.expanded());
    }

    @Test
    void doublesExpandToFourMoves() {
        Dice dice = new Dice(4, 4);

        assertTrue(dice.isDouble());
        assertEquals(List.of(4, 4, 4, 4), dice.expanded());
    }

    @Test
    void diceValuesMustBeBetweenOneAndSix() {
        assertThrows(IllegalArgumentException.class, () -> new Dice(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Dice(1, 7));
    }

    @Test
    void expandedValuesAreImmutable() {
        List<Integer> expanded = new Dice(3, 3).expanded();

        assertThrows(UnsupportedOperationException.class, () -> expanded.add(3));
    }
}
