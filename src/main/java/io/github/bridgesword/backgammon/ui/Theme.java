package io.github.bridgesword.backgammon.ui;

import java.awt.Color;
import java.awt.Font;

final class Theme {
    static final Color APP_BACKGROUND = new Color(0x121A1D);
    static final Color PANEL = new Color(0x1B272B);
    static final Color PANEL_ALT = new Color(0x223237);
    static final Color BOARD_FRAME = new Color(0x8B5E3C);
    static final Color BOARD_INNER = new Color(0xD8B98A);
    static final Color POINT_DARK = new Color(0x225B59);
    static final Color POINT_LIGHT = new Color(0xB84A3A);
    static final Color WHITE_CHECKER = new Color(0xF4EBDD);
    static final Color BLACK_CHECKER = new Color(0x263238);
    static final Color GOLD = new Color(0xE9B44C);
    static final Color TEXT = new Color(0xF4F1EA);
    static final Color MUTED_TEXT = new Color(0xAFC0C4);
    static final Color SUCCESS = new Color(0x65C18C);
    static final Color DANGER = new Color(0xE36D60);

    static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    static final Font HEADING_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private Theme() {
    }
}
