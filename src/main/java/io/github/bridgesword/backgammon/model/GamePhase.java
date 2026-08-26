package io.github.bridgesword.backgammon.model;

/** The phase of a stateful game session. */
public enum GamePhase {
    /** The current player must roll before moving. */
    WAITING_FOR_ROLL,
    /** Dice are available and the current player may move. */
    IN_TURN,
    /** One player has borne off all 15 checkers. */
    GAME_OVER
}
