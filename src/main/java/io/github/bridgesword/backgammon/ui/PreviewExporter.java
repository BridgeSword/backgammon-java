package io.github.bridgesword.backgammon.ui;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/** Generates the README screenshot from the real Swing view. */
public final class PreviewExporter {
    private static final int PREVIEW_WIDTH = 1280;
    private static final int PREVIEW_HEIGHT = 900;

    private PreviewExporter() {
    }

    /**
     * Renders the real game panel to a PNG for the project README.
     *
     * @param args one output PNG path
     * @throws Exception when Swing initialization or image output fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected one output PNG path");
        }
        System.setProperty("java.awt.headless", "true");
        installLookAndFeel();
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        AtomicReference<Exception> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                GamePanel panel = GamePanel.preview();
                panel.setSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
                layoutTree(panel);
                BufferedImage image = new BufferedImage(
                        PREVIEW_WIDTH,
                        PREVIEW_HEIGHT,
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    panel.printAll(graphics);
                } finally {
                    graphics.dispose();
                }
                Files.createDirectories(output.getParent());
                if (!ImageIO.write(image, "png", output.toFile())) {
                    throw new IllegalStateException("No PNG writer is available");
                }
            } catch (Exception exception) {
                failure.set(exception);
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
        System.out.println("Wrote " + output);
    }

    private static void layoutTree(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo lookAndFeel : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(lookAndFeel.getName())) {
                    UIManager.setLookAndFeel(lookAndFeel.getClassName());
                    return;
                }
            }
        } catch (Exception ignored) {
            // Swing's default look and feel is sufficient for the preview.
        }
    }
}
