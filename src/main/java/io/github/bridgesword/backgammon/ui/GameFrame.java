package io.github.bridgesword.backgammon.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** Top-level desktop window. */
public final class GameFrame extends JFrame {
    /** Game content hosted by this window. */
    private final GamePanel gamePanel = new GamePanel();

    /** Creates a centered, resizable game window. */
    public GameFrame() {
        super("Backgammon Java");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(gamePanel);
        setMinimumSize(new Dimension(1040, 740));
        setSize(1280, 900);
        setLocationByPlatform(true);
        setLocationRelativeTo(null);
        installShortcuts();
    }

    private void installShortcuts() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK),
                "new-game");
        getRootPane().getActionMap().put("new-game", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                gamePanel.startNewGame();
            }
        });
    }
}
