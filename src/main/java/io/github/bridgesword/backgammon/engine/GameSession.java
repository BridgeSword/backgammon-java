package io.github.bridgesword.backgammon.engine;

import io.github.bridgesword.backgammon.model.Board;
import io.github.bridgesword.backgammon.model.Dice;
import io.github.bridgesword.backgammon.model.GamePhase;
import io.github.bridgesword.backgammon.model.GameResult;
import io.github.bridgesword.backgammon.model.GameSnapshot;
import io.github.bridgesword.backgammon.model.Move;
import io.github.bridgesword.backgammon.model.MoveSequence;
import io.github.bridgesword.backgammon.model.Player;
import io.github.bridgesword.backgammon.model.Point;
import io.github.bridgesword.backgammon.model.TurnEvent;
import io.github.bridgesword.backgammon.model.TurnOutcome;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Thread-safe, stateful controller for one standard backgammon game.
 *
 * <p>A newly constructed standard session immediately performs the opening
 * roll: each player rolls one die, ties are rerolled, the higher die selects the
 * first player, and both values become that player's first turn. Later turns
 * begin with {@link #roll()}.</p>
 *
 * <p>Interactive moves are accepted only when they are the first move of at
 * least one maximal legal turn sequence. This prevents a player from choosing a
 * locally legal move that would illegally waste another die.</p>
 */
public final class GameSession {
    private final Random random;
    private final Deque<SessionState> undoStates = new ArrayDeque<>();

    private Board board;
    private Player currentPlayer;
    private Dice currentRoll;
    private List<Integer> remainingDice = List.of();
    private GamePhase phase;
    private GameResult result;
    private TurnEvent lastEvent;

    /**
     * Creates a standard game using a new nondeterministic random source.
     */
    public GameSession() {
        this(new Random());
    }

    /**
     * Creates a standard game and performs its opening roll with the supplied
     * random source. Supplying a seeded {@link Random} makes all rolls
     * reproducible.
     *
     * @param random random source used for every die roll
     */
    public GameSession(Random random) {
        this.random = Objects.requireNonNull(random, "random");
        newGame();
    }

    /**
     * Creates a session around a validated position, waiting for the specified
     * player to roll. This constructor is useful for saved games, examples, and
     * focused rule scenarios; it does not perform an opening contest.
     *
     * @param board starting board
     * @param currentPlayer player who rolls next
     * @param random random source used for dice
     */
    public GameSession(Board board, Player currentPlayer, Random random) {
        this.random = Objects.requireNonNull(random, "random");
        this.board = Objects.requireNonNull(board, "board");
        this.currentPlayer = Objects.requireNonNull(currentPlayer, "currentPlayer");
        currentRoll = null;
        remainingDice = List.of();
        result = null;
        phase = GamePhase.WAITING_FOR_ROLL;
        lastEvent = new TurnEvent(
                TurnOutcome.GAME_READY,
                currentPlayer,
                currentPlayer.displayName() + " rolls next.");
    }

    /**
     * Resets to the standard board and performs the official opening roll. Ties
     * are rerolled until one player has the higher die.
     *
     * @return the new immutable session snapshot
     */
    public synchronized GameSnapshot newGame() {
        board = Board.initial();
        result = null;
        undoStates.clear();

        int whiteDie;
        int blackDie;
        do {
            whiteDie = rollOneDie();
            blackDie = rollOneDie();
        } while (whiteDie == blackDie);

        currentPlayer = whiteDie > blackDie ? Player.WHITE : Player.BLACK;
        currentRoll = new Dice(whiteDie, blackDie);
        remainingDice = currentRoll.expanded();
        phase = GamePhase.IN_TURN;
        lastEvent = new TurnEvent(
                TurnOutcome.OPENING_ROLL,
                currentPlayer,
                "Opening roll: White " + whiteDie + ", Black " + blackDie
                        + ". " + currentPlayer.displayName() + " moves first.");
        return snapshot();
    }

    /**
     * Rolls two dice for the current player. A double supplies four equal moves.
     * If no die can be played, the turn is passed automatically and
     * {@link GameSnapshot#lastEvent()} reports
     * {@link TurnOutcome#NO_LEGAL_MOVES}.
     *
     * @return the immutable state after rolling and any automatic pass
     * @throws IllegalStateException unless the session is waiting for a roll
     */
    public synchronized GameSnapshot roll() {
        requirePhase(GamePhase.WAITING_FOR_ROLL, "Dice can only be rolled at the start of a turn");
        undoStates.clear();
        Player rollingPlayer = currentPlayer;
        currentRoll = new Dice(rollOneDie(), rollOneDie());
        remainingDice = currentRoll.expanded();
        phase = GamePhase.IN_TURN;
        lastEvent = new TurnEvent(
                TurnOutcome.ROLLED,
                rollingPlayer,
                rollingPlayer.displayName() + " rolled "
                        + currentRoll.first() + "-" + currentRoll.second() + ".");

        if (BackgammonRules.legalTurnSequences(board, rollingPlayer, remainingDice).isEmpty()) {
            remainingDice = List.of();
            currentPlayer = rollingPlayer.opponent();
            phase = GamePhase.WAITING_FOR_ROLL;
            lastEvent = new TurnEvent(
                    TurnOutcome.NO_LEGAL_MOVES,
                    rollingPlayer,
                    rollingPlayer.displayName() + " rolled "
                            + currentRoll.first() + "-" + currentRoll.second()
                            + " but had no legal move; turn passed.");
        }
        return snapshot();
    }

    /**
     * Returns all complete maximal sequences available from the current state.
     *
     * @return immutable legal sequences, or an empty list outside an active turn
     */
    public synchronized List<MoveSequence> legalSequences() {
        if (phase != GamePhase.IN_TURN) {
            return List.of();
        }
        return BackgammonRules.legalTurnSequences(board, currentPlayer, remainingDice);
    }

    /**
     * Returns every allowed next move. Each result is the first step of at least
     * one complete maximal legal sequence.
     *
     * @return immutable distinct legal first moves
     */
    public synchronized List<Move> legalMoves() {
        if (phase != GamePhase.IN_TURN) {
            return List.of();
        }
        return BackgammonRules.legalFirstMoves(board, currentPlayer, remainingDice);
    }

    /**
     * Applies one allowed first move of the current maximal sequences. The turn
     * ends automatically after all usable dice are consumed or no remaining die
     * can be played.
     *
     * @param move move selected by a player or AI
     * @return the immutable state after the move and any automatic turn change
     * @throws IllegalStateException outside an active turn
     * @throws IllegalArgumentException if the move is not an allowed next move
     */
    public synchronized GameSnapshot move(Move move) {
        requirePhase(GamePhase.IN_TURN, "A checker can only move during an active turn");
        Objects.requireNonNull(move, "move");
        List<Move> allowed = legalMoves();
        if (!allowed.contains(move)) {
            throw new IllegalArgumentException(
                    "Move is not a legal maximal-sequence first step: " + move);
        }

        undoStates.push(captureState());
        board = BackgammonRules.apply(board, move);
        remainingDice = withoutOne(remainingDice, move.die());

        if (board.borneOff(currentPlayer) == Board.CHECKERS_PER_PLAYER) {
            result = BackgammonRules.classifyWin(board, currentPlayer);
            remainingDice = List.of();
            phase = GamePhase.GAME_OVER;
            lastEvent = new TurnEvent(
                    TurnOutcome.GAME_WON,
                    currentPlayer,
                    currentPlayer.displayName() + " wins a "
                            + result.winType().name().toLowerCase() + ".");
            return snapshot();
        }

        if (remainingDice.isEmpty()) {
            finishTurn(
                    TurnOutcome.TURN_COMPLETED,
                    move.player().displayName() + " used all playable dice; turn complete.");
        } else if (BackgammonRules.legalTurnSequences(board, currentPlayer, remainingDice).isEmpty()) {
            finishTurn(
                    TurnOutcome.NO_LEGAL_MOVES,
                    move.player().displayName()
                            + " has no legal move for the remaining dice; turn complete.");
        } else {
            lastEvent = new TurnEvent(
                    TurnOutcome.MOVE_APPLIED,
                    currentPlayer,
                    move.toString());
        }
        return snapshot();
    }

    /**
     * Convenience overload for moving between point value objects.
     *
     * @param from source point or bar
     * @param to destination point or off
     * @param die consumed die
     * @return the state after the move
     */
    public synchronized GameSnapshot move(Point from, Point to, int die) {
        return move(new Move(currentPlayer, from, to, die));
    }

    /**
     * Applies a complete sequence through the same validation path as
     * interactive checker moves.
     *
     * @param sequence a currently legal complete sequence
     * @return the state after its moves
     * @throws IllegalArgumentException if any next move is not currently legal
     */
    public synchronized GameSnapshot play(MoveSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        if (!legalSequences().contains(sequence)) {
            throw new IllegalArgumentException("Sequence is not legal in the current session");
        }
        for (Move move : sequence.moves()) {
            move(move);
        }
        return snapshot();
    }

    /**
     * Reverts the most recent checker move. Rolls themselves are not undoable;
     * rolling for the next player commits the preceding turn and clears undo
     * history. Multiple moves from the current or just-completed turn may be
     * undone one at a time.
     *
     * @return restored immutable state
     * @throws IllegalStateException when no checker move can be undone
     */
    public synchronized GameSnapshot undo() {
        if (undoStates.isEmpty()) {
            throw new IllegalStateException("No checker move is available to undo");
        }
        SessionState previous = undoStates.pop();
        restore(previous);
        lastEvent = new TurnEvent(
                TurnOutcome.UNDO,
                currentPlayer,
                "Last checker move undone.");
        return snapshot();
    }

    /**
     * Returns whether {@link #undo()} is currently available.
     *
     * @return undo availability
     */
    public synchronized boolean canUndo() {
        return !undoStates.isEmpty();
    }

    /**
     * Captures a fully immutable view for Swing or another client.
     *
     * @return current snapshot
     */
    public synchronized GameSnapshot snapshot() {
        return new GameSnapshot(
                board,
                currentPlayer,
                currentRoll,
                remainingDice,
                phase,
                result,
                lastEvent,
                !undoStates.isEmpty());
    }

    /**
     * Returns the immutable current board.
     *
     * @return current board
     */
    public synchronized Board board() {
        return board;
    }

    /**
     * Returns the player moving now or rolling next.
     *
     * @return current player
     */
    public synchronized Player currentPlayer() {
        return currentPlayer;
    }

    /**
     * Returns the current session phase.
     *
     * @return current phase
     */
    public synchronized GamePhase phase() {
        return phase;
    }

    private int rollOneDie() {
        return random.nextInt(6) + 1;
    }

    private void finishTurn(TurnOutcome outcome, String message) {
        Player completedPlayer = currentPlayer;
        remainingDice = List.of();
        currentPlayer = currentPlayer.opponent();
        phase = GamePhase.WAITING_FOR_ROLL;
        lastEvent = new TurnEvent(outcome, completedPlayer, message);
    }

    private SessionState captureState() {
        return new SessionState(
                board,
                currentPlayer,
                currentRoll,
                remainingDice,
                phase,
                result,
                lastEvent);
    }

    private void restore(SessionState state) {
        board = state.board();
        currentPlayer = state.currentPlayer();
        currentRoll = state.currentRoll();
        remainingDice = state.remainingDice();
        phase = state.phase();
        result = state.result();
        lastEvent = state.lastEvent();
    }

    private static List<Integer> withoutOne(List<Integer> dice, int value) {
        List<Integer> copy = new ArrayList<>(dice);
        if (!copy.remove(Integer.valueOf(value))) {
            throw new IllegalStateException("Move consumed an unavailable die: " + value);
        }
        return List.copyOf(copy);
    }

    private void requirePhase(GamePhase expected, String message) {
        if (phase != expected) {
            throw new IllegalStateException(message + " (phase=" + phase + ")");
        }
    }

    private record SessionState(
            Board board,
            Player currentPlayer,
            Dice currentRoll,
            List<Integer> remainingDice,
            GamePhase phase,
            GameResult result,
            TurnEvent lastEvent) {
        private SessionState {
            remainingDice = List.copyOf(remainingDice);
        }
    }
}
