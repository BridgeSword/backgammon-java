package io.github.bridgesword.backgammon.ui;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GamePanelSmokeTest {
    @Test
    void completeGameViewCanLayoutAndPaintWithoutADisplay() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> {
            try {
                GamePanel panel = GamePanel.preview();
                panel.setSize(1280, 900);
                layoutTree(panel);
                BufferedImage image = new BufferedImage(1280, 900, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    panel.printAll(graphics);
                } finally {
                    graphics.dispose();
                }
                assertNotNull(panel.getAccessibleContext());
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        }));

        if (failure.get() != null) {
            throw new AssertionError("Swing preview failed", failure.get());
        }
    }

    private static void layoutTree(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
    }
}
