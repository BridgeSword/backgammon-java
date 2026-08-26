package io.github.bridgesword.backgammon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BoardTest {
    @Test
    void initialBoardHasTheCanonicalPositionAndThirtyCheckers() {
        Board board = Board.initial();

        assertEquals(2, board.countAt(24, Player.WHITE));
        assertEquals(5, board.countAt(13, Player.WHITE));
        assertEquals(3, board.countAt(8, Player.WHITE));
        assertEquals(5, board.countAt(6, Player.WHITE));

        assertEquals(2, board.countAt(1, Player.BLACK));
        assertEquals(5, board.countAt(12, Player.BLACK));
        assertEquals(3, board.countAt(17, Player.BLACK));
        assertEquals(5, board.countAt(19, Player.BLACK));

        assertEquals(15, checkerTotal(board, Player.WHITE));
        assertEquals(15, checkerTotal(board, Player.BLACK));
        assertEquals(0, board.bar(Player.WHITE));
        assertEquals(0, board.bar(Player.BLACK));
        assertEquals(0, board.borneOff(Player.WHITE));
        assertEquals(0, board.borneOff(Player.BLACK));
        assertEquals(167, board.pipCount(Player.WHITE));
        assertEquals(167, board.pipCount(Player.BLACK));
    }

    @Test
    void builderRequiresExactlyFifteenCheckersPerPlayer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Board.builder()
                        .point(1, Player.WHITE, 14)
                        .point(24, Player.BLACK, 15)
                        .build());
    }

    @Test
    void builderCopyDoesNotMutateItsSource() {
        Board initial = Board.initial();
        Board changed = Board.builder(initial)
                .clearPoint(24)
                .point(23, Player.WHITE, 2)
                .build();

        assertEquals(2, initial.countAt(24, Player.WHITE));
        assertEquals(0, initial.countAt(23, Player.WHITE));
        assertEquals(0, changed.countAt(24, Player.WHITE));
        assertEquals(2, changed.countAt(23, Player.WHITE));
        assertNotEquals(initial, changed);
    }

    @Test
    void returnedPointArraysAreDefensiveCopies() {
        Board board = Board.initial();
        int[] white = board.points(Player.WHITE);
        int[] signed = board.signedPoints();

        Arrays.fill(white, 0);
        Arrays.fill(signed, 0);

        assertEquals(2, board.countAt(24, Player.WHITE));
        assertEquals(-2, board.signedPoints()[0]);
    }

    @Test
    void allInHomeRequiresNoOutsideOrBarCheckers() {
        Board allHome = Board.builder()
                .point(1, Player.WHITE, 5)
                .point(3, Player.WHITE, 5)
                .point(6, Player.WHITE, 5)
                .point(19, Player.BLACK, 5)
                .point(22, Player.BLACK, 5)
                .point(24, Player.BLACK, 5)
                .build();
        assertTrue(allHome.allInHome(Player.WHITE));
        assertTrue(allHome.allInHome(Player.BLACK));

        Board whiteOutside = Board.builder(allHome)
                .point(6, Player.WHITE, 4)
                .point(7, Player.WHITE, 1)
                .build();
        assertFalse(whiteOutside.allInHome(Player.WHITE));

        Board blackOnBar = Board.builder(allHome)
                .point(24, Player.BLACK, 4)
                .bar(Player.BLACK, 1)
                .build();
        assertFalse(blackOnBar.allInHome(Player.BLACK));
    }

    @Test
    void builderPreventsNegativeCountsAndSharedPoints() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Board.builder().bar(Player.WHITE, -1));

        Board board = Board.builder()
                .point(8, Player.WHITE, 15)
                .point(8, Player.BLACK, 15)
                .borneOff(Player.WHITE, 15)
                .build();
        assertEquals(0, board.countAt(8, Player.WHITE));
        assertEquals(15, board.countAt(8, Player.BLACK));
    }

    private static int checkerTotal(Board board, Player player) {
        return Arrays.stream(board.points(player)).sum()
                + board.bar(player)
                + board.borneOff(player);
    }
}
