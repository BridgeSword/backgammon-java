package io.github.bridgesword.backgammon.ai;

/** Available computer-player strengths. */
public enum Difficulty {
    /** Selects uniformly from all legal complete turn sequences. */
    EASY,
    /** Uses a positional heuristic covering race, safety, blocks, hits, and bearing off. */
    MEDIUM,
    /** Adds the expected best opponent response over all 36 possible next rolls. */
    HARD
}
