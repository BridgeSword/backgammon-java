package io.github.bridgesword.backgammon.engine;

import io.github.bridgesword.backgammon.model.Board;
import io.github.bridgesword.backgammon.model.Dice;
import io.github.bridgesword.backgammon.model.GameResult;
import io.github.bridgesword.backgammon.model.Move;
import io.github.bridgesword.backgammon.model.MoveSequence;
import io.github.bridgesword.backgammon.model.Player;
import io.github.bridgesword.backgammon.model.Point;
import io.github.bridgesword.backgammon.model.WinType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Stateless implementation of standard backgammon checker-play rules.
 *
 * <p>The engine enforces bar priority, blocked points and hits, exact and
 * oversized bearing off, four moves for doubles, maximum dice usage, and the
 * higher-die rule when only one of two different dice can be played.</p>
 */
public final class BackgammonRules {
    private BackgammonRules() {
    }

    /**
     * Generates every legal one-checker move for a particular die. This method
     * applies bar priority but does not apply whole-turn maximum-use filtering;
     * use {@link #legalTurnSequences(Board, Player, List)} for turn play.
     *
     * @param board current board
     * @param player moving player
     * @param die die value from 1 through 6
     * @return immutable, deterministically ordered legal moves
     */
    public static List<Move> legalMoves(Board board, Player player, int die) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(player, "player");
        requireDie(die);

        if (board.bar(player) > 0) {
            int destination = player.entryPoint(die);
            if (isOpen(board, destination, player)) {
                return List.of(new Move(player, Point.BAR, Point.board(destination), die));
            }
            return List.of();
        }

        List<Move> result = new ArrayList<>();
        if (player == Player.WHITE) {
            for (int point = Board.POINT_COUNT; point >= 1; point--) {
                addMoveFromPoint(board, player, die, point, result);
            }
        } else {
            for (int point = 1; point <= Board.POINT_COUNT; point++) {
                addMoveFromPoint(board, player, die, point, result);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Generates all complete legal turn sequences for a two-die roll. Doubles
     * automatically provide four moves.
     *
     * @param board current board
     * @param player moving player
     * @param dice rolled dice
     * @return every maximal legal sequence, or an empty list when no die can be played
     */
    public static List<MoveSequence> legalTurnSequences(Board board, Player player, Dice dice) {
        Objects.requireNonNull(dice, "dice");
        return legalTurnSequences(board, player, dice.expanded());
    }

    /**
     * Generates all complete legal turn sequences for the supplied unconsumed
     * die values. The result retains only sequences using the greatest possible
     * number of dice. When exactly one of two distinct dice can be used, only
     * sequences using the higher playable die are retained.
     *
     * @param board current board
     * @param player moving player
     * @param dice remaining die values, normally two values or four equal values
     * @return every maximal legal sequence, or an empty list when no die can be played
     */
    public static List<MoveSequence> legalTurnSequences(
            Board board,
            Player player,
            List<Integer> dice) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(dice, "dice");
        List<Integer> checkedDice = List.copyOf(dice);
        checkedDice.forEach(BackgammonRules::requireDie);
        if (checkedDice.isEmpty()) {
            return List.of();
        }

        List<MoveSequence> candidates = new ArrayList<>();
        collectSequences(board, player, checkedDice, new ArrayList<>(), candidates);
        int maximumMoves = candidates.stream()
                .mapToInt(sequence -> sequence.moves().size())
                .max()
                .orElse(0);
        if (maximumMoves == 0) {
            return List.of();
        }

        List<MoveSequence> filtered = candidates.stream()
                .filter(sequence -> sequence.moves().size() == maximumMoves)
                .toList();

        // Official rule: when only one of two unequal dice can be played, the
        // higher one must be used if it is playable.
        if (maximumMoves == 1 && new HashSet<>(checkedDice).size() > 1) {
            int highestPlayableDie = filtered.stream()
                    .mapToInt(sequence -> sequence.firstMove().die())
                    .max()
                    .orElseThrow();
            filtered = filtered.stream()
                    .filter(sequence -> sequence.firstMove().die() == highestPlayableDie)
                    .toList();
        }

        Map<List<Move>, MoveSequence> unique = new LinkedHashMap<>();
        for (MoveSequence sequence : filtered) {
            unique.putIfAbsent(sequence.moves(), sequence);
        }
        return List.copyOf(unique.values());
    }

    /**
     * Returns the distinct legal first moves of every maximal turn sequence.
     * Choosing one of these moves can never forfeit a playable die later in the
     * same turn.
     *
     * @param board current board
     * @param player moving player
     * @param remainingDice unconsumed die values
     * @return immutable distinct first moves
     */
    public static List<Move> legalFirstMoves(
            Board board,
            Player player,
            List<Integer> remainingDice) {
        Set<Move> result = new LinkedHashSet<>();
        for (MoveSequence sequence : legalTurnSequences(board, player, remainingDice)) {
            result.add(sequence.firstMove());
        }
        return List.copyOf(result);
    }

    /**
     * Applies one legal checker move. Whole-turn dice-order restrictions are not
     * considered; callers accepting interactive input should first check
     * {@link #legalFirstMoves(Board, Player, List)}.
     *
     * @param board current board
     * @param move proposed checker move
     * @return the immutable resulting board
     * @throws IllegalArgumentException if the move is illegal for its stated die
     */
    public static Board apply(Board board, Move move) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(move, "move");
        if (!legalMoves(board, move.player(), move.die()).contains(move)) {
            throw new IllegalArgumentException("Illegal move: " + move);
        }
        return applyUnchecked(board, move);
    }

    /**
     * Classifies a completed game according to standard single, gammon, and
     * backgammon scoring.
     *
     * @param board completed board
     * @param winner player who has borne off all 15 checkers
     * @return the classified result
     * @throws IllegalArgumentException if the supplied player has not won
     */
    public static GameResult classifyWin(Board board, Player winner) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(winner, "winner");
        if (board.borneOff(winner) != Board.CHECKERS_PER_PLAYER) {
            throw new IllegalArgumentException(winner + " has not borne off all checkers");
        }

        Player loser = winner.opponent();
        if (board.borneOff(loser) > 0) {
            return new GameResult(winner, WinType.SINGLE);
        }

        boolean loserInWinnersHome = false;
        for (int point = 1; point <= Board.POINT_COUNT; point++) {
            if (winner.isHomePoint(point) && board.countAt(point, loser) > 0) {
                loserInWinnersHome = true;
                break;
            }
        }
        WinType type = board.bar(loser) > 0 || loserInWinnersHome
                ? WinType.BACKGAMMON
                : WinType.GAMMON;
        return new GameResult(winner, type);
    }

    private static void collectSequences(
            Board board,
            Player player,
            List<Integer> dice,
            List<Move> prefix,
            List<MoveSequence> output) {
        boolean advanced = false;
        Set<Integer> attemptedValues = new HashSet<>();
        for (int dieIndex = 0; dieIndex < dice.size(); dieIndex++) {
            int die = dice.get(dieIndex);
            if (!attemptedValues.add(die)) {
                continue;
            }
            for (Move move : legalMoves(board, player, die)) {
                advanced = true;
                List<Integer> nextDice = new ArrayList<>(dice);
                nextDice.remove(dieIndex);
                List<Move> nextPrefix = new ArrayList<>(prefix);
                nextPrefix.add(move);
                collectSequences(
                        applyUnchecked(board, move),
                        player,
                        nextDice,
                        nextPrefix,
                        output);
            }
        }
        if (!advanced) {
            output.add(new MoveSequence(prefix, board));
        }
    }

    private static void addMoveFromPoint(
            Board board,
            Player player,
            int die,
            int source,
            List<Move> output) {
        if (board.countAt(source, player) == 0) {
            return;
        }
        int destination = source + player.direction() * die;
        if (destination >= 1 && destination <= Board.POINT_COUNT) {
            if (isOpen(board, destination, player)) {
                output.add(new Move(player, Point.board(source), Point.board(destination), die));
            }
            return;
        }
        if (canBearOff(board, player, source, die)) {
            output.add(new Move(player, Point.board(source), Point.OFF, die));
        }
    }

    private static boolean canBearOff(Board board, Player player, int source, int die) {
        if (!board.allInHome(player) || !player.isHomePoint(source)) {
            return false;
        }
        int distance = player.bearingOffDistance(source);
        if (die == distance) {
            return true;
        }
        if (die < distance) {
            return false;
        }

        if (player == Player.WHITE) {
            for (int fartherPoint = source + 1; fartherPoint <= 6; fartherPoint++) {
                if (board.countAt(fartherPoint, player) > 0) {
                    return false;
                }
            }
        } else {
            for (int fartherPoint = source - 1; fartherPoint >= 19; fartherPoint--) {
                if (board.countAt(fartherPoint, player) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isOpen(Board board, int destination, Player player) {
        return board.countAt(destination, player.opponent()) <= 1;
    }

    private static Board applyUnchecked(Board board, Move move) {
        Player player = move.player();
        Player opponent = player.opponent();
        Board.Builder builder = Board.builder(board);

        if (move.from().kind() == Point.Kind.BAR) {
            builder.bar(player, board.bar(player) - 1);
        } else {
            int source = move.from().number();
            builder.point(source, player, board.countAt(source, player) - 1);
        }

        if (move.to().kind() == Point.Kind.OFF) {
            builder.borneOff(player, board.borneOff(player) + 1);
        } else {
            int destination = move.to().number();
            if (board.countAt(destination, opponent) == 1) {
                builder.point(destination, opponent, 0);
                builder.bar(opponent, board.bar(opponent) + 1);
            }
            builder.point(destination, player, board.countAt(destination, player) + 1);
        }
        return builder.build();
    }

    private static void requireDie(int die) {
        if (die < 1 || die > 6) {
            throw new IllegalArgumentException("Die must be between 1 and 6: " + die);
        }
    }
}
