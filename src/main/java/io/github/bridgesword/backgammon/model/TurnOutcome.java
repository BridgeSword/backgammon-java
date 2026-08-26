package io.github.bridgesword.backgammon.model;

/** A machine-readable description of the most recent session event. */
public enum TurnOutcome {
    /** A custom position is ready and waiting for its first roll. */
    GAME_READY,
    /** The opening contest selected the first player and supplied the first turn's dice. */
    OPENING_ROLL,
    /** The current player rolled and has one or more legal moves. */
    ROLLED,
    /** A checker moved and at least one further legal move remains. */
    MOVE_APPLIED,
    /** The player's dice were fully consumed and the turn ended normally. */
    TURN_COMPLETED,
    /** No legal move was available, so the turn ended automatically. */
    NO_LEGAL_MOVES,
    /** A player bore off the final checker. */
    GAME_WON,
    /** The most recent checker move was undone. */
    UNDO
}
