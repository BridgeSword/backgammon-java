package io.github.bridgesword.backgammon.ai;

import io.github.bridgesword.backgammon.engine.BackgammonRules;
import io.github.bridgesword.backgammon.model.Board;
import io.github.bridgesword.backgammon.model.Dice;
import io.github.bridgesword.backgammon.model.MoveSequence;
import io.github.bridgesword.backgammon.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Dependency-free computer player with three deterministic, seedable levels.
 *
 * <p>Easy chooses randomly. Medium evaluates the resulting board using pip
 * count, borne-off checkers, bars, made points, blots, and home-board strength.
 * Hard additionally evaluates the opponent's strongest reply for every possible
 * next roll, weighted by its true two-dice probability.</p>
 */
public final class BackgammonAi {
    private static final double WIN_SCORE = 1_000_000.0;
    private static final double SCORE_EPSILON = 1.0e-9;

    private final Random random;

    /**
     * Creates a computer player using a new nondeterministic random source for
     * Easy choices and equal-score tie breaks.
     */
    public BackgammonAi() {
        this(new Random());
    }

    /**
     * Creates a reproducible computer player.
     *
     * @param random source used for random selection and tie breaks
     */
    public BackgammonAi(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Selects a complete legal turn sequence.
     *
     * @param board current board
     * @param player computer-controlled player
     * @param remainingDice unconsumed die values
     * @param difficulty requested playing strength
     * @return a selected sequence, or empty when no die can be played
     */
    public Optional<MoveSequence> chooseSequence(
            Board board,
            Player player,
            List<Integer> remainingDice,
            Difficulty difficulty) {
        return chooseSequence(board, player, remainingDice, difficulty, random);
    }

    /**
     * Selects a complete legal turn sequence using a two-die roll.
     *
     * @param board current board
     * @param player computer-controlled player
     * @param dice current roll
     * @param difficulty requested playing strength
     * @return a selected sequence, or empty when no die can be played
     */
    public Optional<MoveSequence> chooseSequence(
            Board board,
            Player player,
            Dice dice,
            Difficulty difficulty) {
        Objects.requireNonNull(dice, "dice");
        return chooseSequence(board, player, dice.expanded(), difficulty, random);
    }

    /**
     * Stateless convenience entry point. Passing the same position, dice, level,
     * and identically seeded {@link Random} produces the same selection.
     *
     * @param board current board
     * @param player computer-controlled player
     * @param remainingDice unconsumed die values
     * @param difficulty requested playing strength
     * @param random source used for random selection and tie breaks
     * @return a selected sequence, or empty when no die can be played
     */
    public static Optional<MoveSequence> chooseSequence(
            Board board,
            Player player,
            List<Integer> remainingDice,
            Difficulty difficulty,
            Random random) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(remainingDice, "remainingDice");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(random, "random");

        List<MoveSequence> legal = BackgammonRules.legalTurnSequences(
                board,
                player,
                remainingDice);
        if (legal.isEmpty()) {
            return Optional.empty();
        }
        if (difficulty == Difficulty.EASY) {
            return Optional.of(legal.get(random.nextInt(legal.size())));
        }

        Map<Board, Double> hardReplyCache = difficulty == Difficulty.HARD
                ? new HashMap<>()
                : Map.of();
        List<ScoredSequence> scored = new ArrayList<>(legal.size());
        double bestScore = Double.NEGATIVE_INFINITY;
        for (MoveSequence sequence : legal) {
            Board resultingBoard = sequence.resultingBoard();
            double score = difficulty == Difficulty.MEDIUM
                    ? evaluate(resultingBoard, player)
                    : hardReplyCache.computeIfAbsent(
                            resultingBoard,
                            position -> hardScore(position, player));
            scored.add(new ScoredSequence(sequence, score));
            bestScore = Math.max(bestScore, score);
        }

        List<MoveSequence> best = new ArrayList<>();
        for (ScoredSequence candidate : scored) {
            if (Math.abs(candidate.score() - bestScore) <= SCORE_EPSILON) {
                best.add(candidate.sequence());
            }
        }
        return Optional.of(best.get(random.nextInt(best.size())));
    }

    /**
     * Returns the Medium-level positional score from one player's perspective.
     * Larger values are better for that player. This is exposed for diagnostics,
     * analysis displays, and reproducible AI tests.
     *
     * @param board board to evaluate
     * @param perspective player for whom positive values are favorable
     * @return positional score
     */
    public static double evaluate(Board board, Player perspective) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(perspective, "perspective");
        Player opponent = perspective.opponent();
        if (board.borneOff(perspective) == Board.CHECKERS_PER_PLAYER) {
            return WIN_SCORE;
        }
        if (board.borneOff(opponent) == Board.CHECKERS_PER_PLAYER) {
            return -WIN_SCORE;
        }

        double score = 0.0;
        score += 165.0 * (board.borneOff(perspective) - board.borneOff(opponent));
        score += board.pipCount(opponent) - board.pipCount(perspective);
        score += 38.0 * board.bar(opponent) - 45.0 * board.bar(perspective);

        for (int point = 1; point <= Board.POINT_COUNT; point++) {
            int own = board.countAt(point, perspective);
            int theirs = board.countAt(point, opponent);
            if (own >= 2) {
                score += 7.0 + Math.min(own - 2, 3);
                if (perspective.isHomePoint(point)) {
                    score += 5.0;
                }
            } else if (own == 1) {
                score -= 5.0;
            }
            if (theirs >= 2) {
                score -= 7.0 + Math.min(theirs - 2, 3);
                if (opponent.isHomePoint(point)) {
                    score -= 5.0;
                }
            } else if (theirs == 1) {
                score += 5.0;
            }
        }
        return score;
    }

    private static double hardScore(Board board, Player perspective) {
        if (board.borneOff(perspective) == Board.CHECKERS_PER_PLAYER) {
            return WIN_SCORE;
        }
        Player opponent = perspective.opponent();
        double expectedAfterReply = 0.0;
        for (int first = 1; first <= 6; first++) {
            for (int second = first; second <= 6; second++) {
                int outcomeMultiplicity = first == second ? 1 : 2;
                Dice replyRoll = new Dice(first, second);
                List<MoveSequence> replies = BackgammonRules.legalTurnSequences(
                        board,
                        opponent,
                        replyRoll);
                double replyValue;
                if (replies.isEmpty()) {
                    replyValue = evaluate(board, perspective);
                } else {
                    // The opponent is assumed to choose the reply least favorable
                    // to the current player.
                    replyValue = replies.stream()
                            .mapToDouble(reply -> evaluate(reply.resultingBoard(), perspective))
                            .min()
                            .orElseThrow();
                }
                expectedAfterReply += outcomeMultiplicity * replyValue / 36.0;
            }
        }
        return 0.15 * evaluate(board, perspective) + 0.85 * expectedAfterReply;
    }

    private record ScoredSequence(MoveSequence sequence, double score) {
    }
}
