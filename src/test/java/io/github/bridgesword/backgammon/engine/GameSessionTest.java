package io.github.bridgesword.backgammon.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bridgesword.backgammon.model.Board;
import io.github.bridgesword.backgammon.model.Dice;
import io.github.bridgesword.backgammon.model.GamePhase;
import io.github.bridgesword.backgammon.model.GameSnapshot;
import io.github.bridgesword.backgammon.model.Move;
import io.github.bridgesword.backgammon.model.MoveSequence;
import io.github.bridgesword.backgammon.model.Player;
import io.github.bridgesword.backgammon.model.Point;
import io.github.bridgesword.backgammon.model.TurnOutcome;
import io.github.bridgesword.backgammon.model.WinType;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GameSessionTest {
    @Test
    void openingRollRerollsTiesAndUsesTheWinningRollAsTheFirstTurn() {
        GameSession session = new GameSession(new DiceSequenceRandom(3, 3, 2, 5));

        GameSnapshot snapshot = session.snapshot();
        assertEquals(Board.initial(), snapshot.board());
        assertEquals(Player.BLACK, snapshot.currentPlayer());
        assertEquals(new Dice(2, 5), snapshot.currentRoll());
        assertEquals(List.of(2, 5), snapshot.remainingDice());
        assertEquals(GamePhase.IN_TURN, snapshot.phase());
        assertEquals(TurnOutcome.OPENING_ROLL, snapshot.lastEvent().outcome());
    }

    @Test
    void customSessionStartsWaitingThenRollsIntoAnActiveTurn() {
        Board board = sparseRaceBoard();
        GameSession session = new GameSession(
                board, Player.WHITE, new DiceSequenceRandom(2, 3));

        GameSnapshot ready = session.snapshot();
        assertEquals(GamePhase.WAITING_FOR_ROLL, ready.phase());
        assertTrue(ready.currentRollOptional().isEmpty());
        assertTrue(ready.remainingDice().isEmpty());
        assertTrue(session.legalMoves().isEmpty());
        assertEquals(TurnOutcome.GAME_READY, ready.lastEvent().outcome());

        GameSnapshot rolled = session.roll();
        assertEquals(GamePhase.IN_TURN, rolled.phase());
        assertEquals(new Dice(2, 3), rolled.currentRoll());
        assertEquals(List.of(2, 3), rolled.remainingDice());
        assertEquals(TurnOutcome.ROLLED, rolled.lastEvent().outcome());
        assertFalse(session.legalSequences().isEmpty());
        assertThrows(IllegalStateException.class, session::roll);
    }

    @Test
    void playingACompleteSequenceConsumesDiceAndHandsOverTheTurn() {
        GameSession session = new GameSession(
                sparseRaceBoard(), Player.WHITE, new DiceSequenceRandom(2, 3));
        session.roll();
        MoveSequence selected = session.legalSequences().get(0);

        GameSnapshot completed = session.play(selected);

        assertEquals(selected.resultingBoard(), completed.board());
        assertEquals(Player.BLACK, completed.currentPlayer());
        assertEquals(GamePhase.WAITING_FOR_ROLL, completed.phase());
        assertTrue(completed.remainingDice().isEmpty());
        assertEquals(TurnOutcome.TURN_COMPLETED, completed.lastEvent().outcome());
        assertTrue(completed.canUndo());
    }

    @Test
    void moveAndUndoRestoreBoardPlayerAndRemainingDice() {
        Board board = sparseRaceBoard();
        GameSession session = new GameSession(
                board, Player.WHITE, new DiceSequenceRandom(2, 3));
        session.roll();
        Move first = move(Player.WHITE, 8, 6, 2);

        GameSnapshot moved = session.move(first);
        assertEquals(GamePhase.IN_TURN, moved.phase());
        assertEquals(List.of(3), moved.remainingDice());
        assertEquals(TurnOutcome.MOVE_APPLIED, moved.lastEvent().outcome());
        assertTrue(moved.canUndo());

        GameSnapshot restored = session.undo();
        assertEquals(board, restored.board());
        assertEquals(Player.WHITE, restored.currentPlayer());
        assertEquals(GamePhase.IN_TURN, restored.phase());
        assertEquals(List.of(2, 3), restored.remainingDice());
        assertEquals(TurnOutcome.UNDO, restored.lastEvent().outcome());
        assertFalse(restored.canUndo());
        assertThrows(IllegalStateException.class, session::undo);
    }

    @Test
    void sessionRejectsLocallyLegalMoveThatWouldWasteAPlayableDie() {
        Board board = Board.builder()
                .bar(Player.WHITE, 1)
                .point(6, Player.WHITE, 1)
                .borneOff(Player.WHITE, 13)
                .point(22, Player.BLACK, 2)
                .point(4, Player.BLACK, 2)
                .borneOff(Player.BLACK, 11)
                .build();
        GameSession session = new GameSession(
                board, Player.WHITE, new DiceSequenceRandom(1, 2));
        session.roll();

        Move shorterOrdering = new Move(Player.WHITE, Point.BAR, Point.board(24), 1);
        assertTrue(BackgammonRules.legalMoves(board, Player.WHITE, 1).contains(shorterOrdering));
        assertFalse(session.legalMoves().contains(shorterOrdering));
        assertThrows(IllegalArgumentException.class, () -> session.move(shorterOrdering));
    }

    @Test
    void rollWithNoLegalEntryAutomaticallyPassesTheTurn() {
        Board.Builder builder = Board.builder()
                .bar(Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .borneOff(Player.BLACK, 3);
        for (int point = 19; point <= 24; point++) {
            builder.point(point, Player.BLACK, 2);
        }
        GameSession session = new GameSession(
                builder.build(), Player.WHITE, new DiceSequenceRandom(2, 5));

        GameSnapshot passed = session.roll();

        assertEquals(Player.BLACK, passed.currentPlayer());
        assertEquals(GamePhase.WAITING_FOR_ROLL, passed.phase());
        assertTrue(passed.remainingDice().isEmpty());
        assertEquals(TurnOutcome.NO_LEGAL_MOVES, passed.lastEvent().outcome());
        assertEquals(Player.WHITE, passed.lastEvent().player());
    }

    @Test
    void bearingOffTheFinalCheckerEndsTheGameAndRecordsWinner() {
        Board board = Board.builder()
                .point(1, Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .point(24, Player.BLACK, 14)
                .borneOff(Player.BLACK, 1)
                .build();
        GameSession session = new GameSession(
                board, Player.WHITE, new DiceSequenceRandom(1, 2));
        session.roll();

        GameSnapshot won = session.move(
                new Move(Player.WHITE, Point.board(1), Point.OFF, 2));

        assertEquals(GamePhase.GAME_OVER, won.phase());
        assertEquals(Player.WHITE, won.currentPlayer());
        assertTrue(won.remainingDice().isEmpty());
        assertEquals(Player.WHITE, won.resultOptional().orElseThrow().winner());
        assertEquals(WinType.SINGLE, won.resultOptional().orElseThrow().winType());
        assertEquals(TurnOutcome.GAME_WON, won.lastEvent().outcome());
        assertTrue(session.legalMoves().isEmpty());
        assertThrows(IllegalStateException.class, session::roll);
    }

    private static Board sparseRaceBoard() {
        return Board.builder()
                .point(8, Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .point(17, Player.BLACK, 1)
                .borneOff(Player.BLACK, 14)
                .build();
    }

    private static Move move(Player player, int from, int to, int die) {
        return new Move(player, Point.board(from), Point.board(to), die);
    }

    private static final class DiceSequenceRandom extends Random {
        private final int[] dice;
        private int index;

        private DiceSequenceRandom(int... dice) {
            this.dice = dice.clone();
        }

        @Override
        public int nextInt(int bound) {
            if (bound != 6 || index >= dice.length) {
                throw new AssertionError("Unexpected random request");
            }
            int die = dice[index++];
            if (die < 1 || die > 6) {
                throw new AssertionError("Test die must be between 1 and 6");
            }
            return die - 1;
        }
    }
}
