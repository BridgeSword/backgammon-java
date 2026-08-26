package io.github.bridgesword.backgammon.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bridgesword.backgammon.engine.BackgammonRules;
import io.github.bridgesword.backgammon.model.Board;
import io.github.bridgesword.backgammon.model.Dice;
import io.github.bridgesword.backgammon.model.MoveSequence;
import io.github.bridgesword.backgammon.model.Player;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BackgammonAiTest {
    @Test
    void everyDifficultyChoosesOnlyLegalSequencesAcrossPlayersAndRolls() {
        Board board = sparseRaceBoard();

        for (Difficulty difficulty : Difficulty.values()) {
            BackgammonAi ai = new BackgammonAi(new Random(20260826L));
            for (Player player : Player.values()) {
                for (int first = 1; first <= 6; first++) {
                    for (int second = first; second <= 6; second++) {
                        Dice dice = new Dice(first, second);
                        List<MoveSequence> legal = BackgammonRules.legalTurnSequences(
                                board, player, dice);
                        Optional<MoveSequence> selected = ai.chooseSequence(
                                board, player, dice, difficulty);

                        if (legal.isEmpty()) {
                            assertTrue(selected.isEmpty(), failureMessage(difficulty, player, dice));
                        } else {
                            assertTrue(selected.isPresent(), failureMessage(difficulty, player, dice));
                            assertTrue(
                                    legal.contains(selected.orElseThrow()),
                                    failureMessage(difficulty, player, dice));
                        }
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void identicalSeedsProduceIdenticalChoices(Difficulty difficulty) {
        Board board = sparseRaceBoard();
        BackgammonAi first = new BackgammonAi(new Random(42L));
        BackgammonAi second = new BackgammonAi(new Random(42L));

        MoveSequence firstChoice = first.chooseSequence(
                        board, Player.WHITE, new Dice(2, 3), difficulty)
                .orElseThrow();
        MoveSequence secondChoice = second.chooseSequence(
                        board, Player.WHITE, new Dice(2, 3), difficulty)
                .orElseThrow();

        assertEquals(firstChoice, secondChoice);
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void aiReturnsEmptyWhenNoDieCanBePlayed(Difficulty difficulty) {
        Board.Builder builder = Board.builder()
                .bar(Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .borneOff(Player.BLACK, 3);
        for (int point = 19; point <= 24; point++) {
            builder.point(point, Player.BLACK, 2);
        }

        Optional<MoveSequence> selected = new BackgammonAi(new Random(7L))
                .chooseSequence(builder.build(), Player.WHITE, new Dice(2, 5), difficulty);

        assertTrue(selected.isEmpty());
    }

    @Test
    void evaluationRewardsProgressAndIsSymmetricByPerspective() {
        Board board = Board.builder()
                .point(3, Player.WHITE, 5)
                .borneOff(Player.WHITE, 10)
                .point(20, Player.BLACK, 10)
                .borneOff(Player.BLACK, 5)
                .build();

        double whiteScore = BackgammonAi.evaluate(board, Player.WHITE);
        double blackScore = BackgammonAi.evaluate(board, Player.BLACK);

        assertTrue(whiteScore > 0.0);
        assertEquals(whiteScore, -blackScore, 1.0e-9);
    }

    private static Board sparseRaceBoard() {
        return Board.builder()
                .point(8, Player.WHITE, 1)
                .borneOff(Player.WHITE, 14)
                .point(17, Player.BLACK, 1)
                .borneOff(Player.BLACK, 14)
                .build();
    }

    private static String failureMessage(
            Difficulty difficulty, Player player, Dice dice) {
        return difficulty + " failed for " + player + " with " + dice;
    }
}
