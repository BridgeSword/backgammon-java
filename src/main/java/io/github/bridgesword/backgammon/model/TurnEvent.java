package io.github.bridgesword.backgammon.model;

import java.util.Objects;

/**
 * A user-displayable and machine-readable session event.
 *
 * @param outcome event category
 * @param player player responsible for or affected by the event
 * @param message concise human-readable explanation
 */
public record TurnEvent(TurnOutcome outcome, Player player, String message) {
    /**
     * Creates an immutable event.
     *
     * @param outcome event category
     * @param player affected player
     * @param message display text
     */
    public TurnEvent {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
    }
}
