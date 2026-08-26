package io.github.bridgesword.backgammon.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bridgesword.backgammon.model.Board;
import io.github.bridgesword.backgammon.model.Dice;
import io.github.bridgesword.backgammon.model.GameResult;
import io.github.bridgesword.backgammon.model.Move;
import io.github.bridgesword.backgammon.model.MoveSequence;
import io.github.bridgesword.backgammon.model.Player;
import io.github.bridgesword.backgammon.model.Point;
import io.github.bridgesword.backgammon.model.WinType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BackgammonRulesTest {
    @Test
    void checkerOnBarMustEnterBeforeAnyBoardCheckerMoves() {
        Board board = Board.builder()
                .bar(Player.WHITE, 1)
                .point(6, Player.WHITE, 1)
                .borneOff(Player.WHITE, 13)
                .borneOff(Player.BLACK, 15)
                .build();

        assertEquals(
                List.of(move(Player.WHITE, Point.BAR, 24, 1)),
                BackgammonRules.legalMoves(board, Player.WHITE, 1));
    }

    @Test
    void twoOpposingCheckersBlockEntryAndDoNotReleaseBarPriority() {
        Board board = Board.builder()
                .bar(Player.WHITE, 1)
                .point(6, Player.WHITE, 14)
                .point(22, Player.BLACK, 2)
                .borneOff(Player.BLACK, 13)
                .build();

        assertTrue(BackgammonRules.legalMoves(board, Player.WHITE, 3).isEmpty());
    }

    @Test
    void enteringOnAnOpposingBlotHitsItToTheBar() {
        Board board = Board.builder()
                .bar(Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .point(22, Player.BLACK, 1)
                .borneOff(Player.BLACK, 14)
                .build();
        Move entry = move(Player.WHITE, Point.BAR, 22, 3);

        Board result = BackgammonRules.apply(board, entry);

        assertEquals(0, result.bar(Player.WHITE));
        assertEquals(1, result.countAt(22, Player.WHITE));
        assertEquals(0, result.countAt(22, Player.BLACK));
        assertEquals(1, result.bar(Player.BLACK));
    }

    @Test
    void ordinaryMoveHitsABlotButCannotLandOnABlockedPoint() {
        Board blot = Board.builder()
                .point(8, Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .point(5, Player.BLACK, 1)
                .borneOff(Player.BLACK, 14)
                .build();
        Move hit = move(Player.WHITE, 8, 5, 3);

        Board result = BackgammonRules.apply(blot, hit);
        assertEquals(1, result.countAt(5, Player.WHITE));
        assertEquals(1, result.bar(Player.BLACK));

        Board blocked = Board.builder()
                .point(8, Player.WHITE, 15)
                .point(5, Player.BLACK, 2)
                .borneOff(Player.BLACK, 13)
                .build();
        assertTrue(BackgammonRules.legalMoves(blocked, Player.WHITE, 3).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> BackgammonRules.apply(blocked, hit));
    }

    @Test
    void exactBearingOffIsAllowedEvenWhenAFartherCheckerRemains() {
        Board board = Board.builder()
                .point(5, Player.WHITE, 1)
                .point(3, Player.WHITE, 14)
                .borneOff(Player.BLACK, 15)
                .build();
        Move exact = new Move(Player.WHITE, Point.board(5), Point.OFF, 5);

        assertTrue(BackgammonRules.legalMoves(board, Player.WHITE, 5).contains(exact));
        assertEquals(1, BackgammonRules.apply(board, exact).borneOff(Player.WHITE));
    }

    @Test
    void oversizedDieBearsOffOnlyTheFarthestOccupiedPoint() {
        Board board = Board.builder()
                .point(3, Player.WHITE, 1)
                .point(2, Player.WHITE, 14)
                .borneOff(Player.BLACK, 15)
                .build();
        Move farthest = new Move(Player.WHITE, Point.board(3), Point.OFF, 4);
        Move nearer = new Move(Player.WHITE, Point.board(2), Point.OFF, 4);

        List<Move> moves = BackgammonRules.legalMoves(board, Player.WHITE, 4);

        assertTrue(moves.contains(farthest));
        assertFalse(moves.contains(nearer));
    }

    @Test
    void oversizedBearingOffIsForbiddenWhileAFartherCheckerExists() {
        Board board = Board.builder()
                .point(5, Player.WHITE, 1)
                .point(3, Player.WHITE, 14)
                .borneOff(Player.BLACK, 15)
                .build();

        List<Move> moves = BackgammonRules.legalMoves(board, Player.WHITE, 4);

        assertFalse(moves.contains(new Move(Player.WHITE, Point.board(3), Point.OFF, 4)));
        assertTrue(moves.contains(move(Player.WHITE, 5, 1, 4)));
    }

    @Test
    void noCheckerMayBearOffUntilEveryCheckerIsHome() {
        Board board = Board.builder()
                .point(7, Player.WHITE, 1)
                .point(1, Player.WHITE, 14)
                .borneOff(Player.BLACK, 15)
                .build();

        List<Move> moves = BackgammonRules.legalMoves(board, Player.WHITE, 1);

        assertEquals(List.of(move(Player.WHITE, 7, 6, 1)), moves);
        assertFalse(moves.contains(new Move(Player.WHITE, Point.board(1), Point.OFF, 1)));
    }

    @Test
    void blackBearingOffMirrorsWhiteBearingOff() {
        Board board = Board.builder()
                .borneOff(Player.WHITE, 15)
                .point(20, Player.BLACK, 1)
                .point(22, Player.BLACK, 14)
                .build();
        Move exact = new Move(Player.BLACK, Point.board(20), Point.OFF, 5);

        assertTrue(BackgammonRules.legalMoves(board, Player.BLACK, 5).contains(exact));
    }

    @Test
    void doublesCanMoveTheSameCheckerFourTimes() {
        Board board = Board.builder()
                .point(8, Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .borneOff(Player.BLACK, 15)
                .build();

        List<MoveSequence> sequences =
                BackgammonRules.legalTurnSequences(board, Player.WHITE, new Dice(1, 1));

        assertEquals(1, sequences.size());
        assertEquals(
                List.of(
                        move(Player.WHITE, 8, 7, 1),
                        move(Player.WHITE, 7, 6, 1),
                        move(Player.WHITE, 6, 5, 1),
                        move(Player.WHITE, 5, 4, 1)),
                sequences.get(0).moves());
        assertEquals(1, sequences.get(0).resultingBoard().countAt(4, Player.WHITE));
    }

    @Test
    void maximumDiceUsageEliminatesAPlayableButShorterOrdering() {
        Board board = Board.builder()
                .bar(Player.WHITE, 1)
                .point(6, Player.WHITE, 1)
                .borneOff(Player.WHITE, 13)
                .point(22, Player.BLACK, 2)
                .point(4, Player.BLACK, 2)
                .borneOff(Player.BLACK, 11)
                .build();

        List<MoveSequence> sequences =
                BackgammonRules.legalTurnSequences(board, Player.WHITE, new Dice(1, 2));

        assertEquals(1, sequences.size());
        assertEquals(
                List.of(
                        move(Player.WHITE, Point.BAR, 23, 2),
                        move(Player.WHITE, 6, 5, 1)),
                sequences.get(0).moves());
        assertEquals(
                List.of(move(Player.WHITE, Point.BAR, 23, 2)),
                BackgammonRules.legalFirstMoves(board, Player.WHITE, List.of(1, 2)));
    }

    @Test
    void higherDieIsMandatoryWhenOnlyOneOfTwoDiceCanBeUsed() {
        Board board = Board.builder()
                .point(8, Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .point(3, Player.BLACK, 2)
                .borneOff(Player.BLACK, 13)
                .build();

        List<MoveSequence> sequences =
                BackgammonRules.legalTurnSequences(board, Player.WHITE, new Dice(2, 3));

        assertEquals(1, sequences.size());
        assertEquals(List.of(move(Player.WHITE, 8, 5, 3)), sequences.get(0).moves());
    }

    @Test
    void bothDiceOrderPermutationsAreGeneratedWhenTheyAreLegal() {
        Board board = Board.builder()
                .point(8, Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .borneOff(Player.BLACK, 15)
                .build();

        List<MoveSequence> sequences =
                BackgammonRules.legalTurnSequences(board, Player.WHITE, new Dice(2, 3));

        Set<List<Move>> expected = Set.of(
                List.of(move(Player.WHITE, 8, 6, 2), move(Player.WHITE, 6, 3, 3)),
                List.of(move(Player.WHITE, 8, 5, 3), move(Player.WHITE, 5, 3, 2)));
        assertEquals(expected, sequences.stream().map(MoveSequence::moves).collect(java.util.stream.Collectors.toSet()));
        assertEquals(1, sequences.stream()
                .map(MoveSequence::resultingBoard)
                .distinct()
                .count());
    }

    @Test
    void completedGamesAreClassifiedAsSingleGammonOrBackgammon() {
        Board single = Board.builder()
                .borneOff(Player.WHITE, 15)
                .point(24, Player.BLACK, 14)
                .borneOff(Player.BLACK, 1)
                .build();
        Board gammon = Board.builder()
                .borneOff(Player.WHITE, 15)
                .point(7, Player.BLACK, 15)
                .build();
        Board barBackgammon = Board.builder()
                .borneOff(Player.WHITE, 15)
                .point(7, Player.BLACK, 14)
                .bar(Player.BLACK, 1)
                .build();
        Board homeBackgammon = Board.builder()
                .borneOff(Player.WHITE, 15)
                .point(7, Player.BLACK, 14)
                .point(6, Player.BLACK, 1)
                .build();

        assertEquals(new GameResult(Player.WHITE, WinType.SINGLE),
                BackgammonRules.classifyWin(single, Player.WHITE));
        assertEquals(new GameResult(Player.WHITE, WinType.GAMMON),
                BackgammonRules.classifyWin(gammon, Player.WHITE));
        assertEquals(new GameResult(Player.WHITE, WinType.BACKGAMMON),
                BackgammonRules.classifyWin(barBackgammon, Player.WHITE));
        assertEquals(new GameResult(Player.WHITE, WinType.BACKGAMMON),
                BackgammonRules.classifyWin(homeBackgammon, Player.WHITE));
    }

    @Test
    void unfinishedBoardCannotBeClassifiedAsAWin() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BackgammonRules.classifyWin(Board.initial(), Player.WHITE));
    }

    private static Move move(Player player, int from, int to, int die) {
        return new Move(player, Point.board(from), Point.board(to), die);
    }

    private static Move move(Player player, Point from, int to, int die) {
        return new Move(player, from, Point.board(to), die);
    }
}
