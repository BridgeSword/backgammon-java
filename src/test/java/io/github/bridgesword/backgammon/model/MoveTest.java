package io.github.bridgesword.backgammon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MoveTest {
    @Test
    void aMoveRecordsThePlayerEndpointsAndConsumedDie() {
        Move move = new Move(Player.WHITE, Point.board(13), Point.board(8), 5);

        assertEquals(Player.WHITE, move.player());
        assertEquals(Point.board(13), move.from());
        assertEquals(Point.board(8), move.to());
        assertEquals(5, move.die());
    }

    @Test
    void structurallyImpossibleMovesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Move(Player.WHITE, Point.OFF, Point.board(1), 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Move(Player.WHITE, Point.board(1), Point.BAR, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Move(Player.WHITE, Point.board(1), Point.board(1), 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Move(Player.WHITE, Point.board(2), Point.board(1), 0));
    }
}
