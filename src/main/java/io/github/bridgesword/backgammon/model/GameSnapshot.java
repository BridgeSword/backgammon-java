package io.github.bridgesword.backgammon.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable, UI-safe view of a {@code GameSession}.
 *
 * @param board immutable board position
 * @param currentPlayer player who is moving or must roll next
 * @param currentRoll most recent roll, or {@code null} before a custom session's first roll
 * @param remainingDice unconsumed die values
 * @param phase current session phase
 * @param result completed result, or {@code null} before game over
 * @param lastEvent most recent session event
 * @param canUndo whether the most recent checker move can be undone
 */
public record GameSnapshot(
        Board board,
        Player currentPlayer,
        Dice currentRoll,
        List<Integer> remainingDice,
        GamePhase phase,
        GameResult result,
        TurnEvent lastEvent,
        boolean canUndo) {

    /**
     * Creates a defensively copied snapshot.
     *
     * @param board board position
     * @param currentPlayer active player
     * @param currentRoll active roll, if any
     * @param remainingDice remaining dice
     * @param phase current phase
     * @param result game result, if complete
     * @param lastEvent most recent event
     * @param canUndo undo availability
     */
    public GameSnapshot {
        board = Objects.requireNonNull(board, "board");
        currentPlayer = Objects.requireNonNull(currentPlayer, "currentPlayer");
        remainingDice = List.copyOf(Objects.requireNonNull(remainingDice, "remainingDice"));
        phase = Objects.requireNonNull(phase, "phase");
        lastEvent = Objects.requireNonNull(lastEvent, "lastEvent");
        if (phase == GamePhase.GAME_OVER && result == null) {
            throw new IllegalArgumentException("A game-over snapshot requires a result");
        }
        if (phase != GamePhase.GAME_OVER && result != null) {
            throw new IllegalArgumentException("An unfinished snapshot cannot contain a result");
        }
    }

    /**
     * Returns the current roll without exposing a nullable value.
     *
     * @return the active roll, if the session has one
     */
    public Optional<Dice> currentRollOptional() {
        return Optional.ofNullable(currentRoll);
    }

    /**
     * Returns the result without exposing a nullable value.
     *
     * @return the result when the game is complete
     */
    public Optional<GameResult> resultOptional() {
        return Optional.ofNullable(result);
    }
}
