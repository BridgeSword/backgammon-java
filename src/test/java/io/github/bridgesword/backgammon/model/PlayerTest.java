package io.github.bridgesword.backgammon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlayerTest {
    @Test
    void playersMoveInOppositeDirectionsAndAreEachOthersOpponent() {
        assertEquals(-1, Player.WHITE.direction());
        assertEquals(1, Player.BLACK.direction());
        assertEquals(Player.BLACK, Player.WHITE.opponent());
        assertEquals(Player.WHITE, Player.BLACK.opponent());
    }

    @Test
    void homeBoardsUseOppositeSixPointQuadrants() {
        assertTrue(Player.WHITE.isHomePoint(1));
        assertTrue(Player.WHITE.isHomePoint(6));
        assertFalse(Player.WHITE.isHomePoint(7));
        assertFalse(Player.WHITE.isHomePoint(24));

        assertTrue(Player.BLACK.isHomePoint(19));
        assertTrue(Player.BLACK.isHomePoint(24));
        assertFalse(Player.BLACK.isHomePoint(18));
        assertFalse(Player.BLACK.isHomePoint(1));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 24, 1",
        "2, 23, 2",
        "3, 22, 3",
        "4, 21, 4",
        "5, 20, 5",
        "6, 19, 6"
    })
    void barEntryMapsEachDieToTheCorrectPoint(int die, int whitePoint, int blackPoint) {
        assertEquals(whitePoint, Player.WHITE.entryPoint(die));
        assertEquals(blackPoint, Player.BLACK.entryPoint(die));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1, 24",
        "2, 2, 23",
        "3, 3, 22",
        "4, 4, 21",
        "5, 5, 20",
        "6, 6, 19"
    })
    void bearingOffDistanceMirrorsAcrossTheBoard(int distance, int whitePoint, int blackPoint) {
        assertEquals(distance, Player.WHITE.bearingOffDistance(whitePoint));
        assertEquals(distance, Player.BLACK.bearingOffDistance(blackPoint));
    }

    @Test
    void entryAndBearingDistanceRejectOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> Player.WHITE.entryPoint(0));
        assertThrows(IllegalArgumentException.class, () -> Player.BLACK.entryPoint(7));
        assertThrows(IllegalArgumentException.class, () -> Player.WHITE.bearingOffDistance(7));
        assertThrows(IllegalArgumentException.class, () -> Player.BLACK.bearingOffDistance(18));
    }
}
