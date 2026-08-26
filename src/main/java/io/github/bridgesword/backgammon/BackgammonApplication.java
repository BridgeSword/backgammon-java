package io.github.bridgesword.backgammon;

import io.github.bridgesword.backgammon.ui.GameFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Desktop entry point for Backgammon Java. */
public final class BackgammonApplication {
    private BackgammonApplication() {
    }

    /**
     * Starts the Swing application on the event-dispatch thread.
     *
     * @param args command-line arguments; currently unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            new GameFrame().setVisible(true);
        });
    }

    private static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo lookAndFeel : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(lookAndFeel.getName())) {
                    UIManager.setLookAndFeel(lookAndFeel.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing's cross-platform default remains fully functional.
        }
    }
}
