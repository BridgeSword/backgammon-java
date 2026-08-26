package io.github.bridgesword.backgammon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PointTest {
    @Test
    void boardBarAndOffLocationsHaveStableLabels() {
        Point point = Point.board(13);

        assertTrue(point.isBoard());
        assertEquals("13", point.displayName());
        assertEquals("13", point.toString());
        assertFalse(Point.BAR.isBoard());
        assertEquals("Bar", Point.BAR.displayName());
        assertEquals("Off", Point.OFF.displayName());
    }

    @Test
    void pointFactoryAcceptsOnlyTheTwentyFourBoardPoints() {
        assertEquals(1, Point.board(1).number());
        assertEquals(24, Point.board(24).number());
        assertThrows(IllegalArgumentException.class, () -> Point.board(0));
        assertThrows(IllegalArgumentException.class, () -> Point.board(25));
    }

    @Test
    void specialLocationsCannotCarryAPointNumber() {
        assertThrows(IllegalArgumentException.class, () -> new Point(Point.Kind.BAR, 1));
        assertThrows(IllegalArgumentException.class, () -> new Point(Point.Kind.OFF, 24));
    }
}
