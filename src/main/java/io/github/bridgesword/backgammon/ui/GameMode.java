package io.github.bridgesword.backgammon.ui;

enum GameMode {
    HUMAN_VS_AI("Human vs AI"),
    LOCAL_TWO_PLAYER("Local two player");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
