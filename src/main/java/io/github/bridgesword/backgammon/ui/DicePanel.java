package io.github.bridgesword.backgammon.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

final class DicePanel extends JComponent {
    private static final int DIE_SIZE = 44;
    private static final int GAP = 10;
    private List<Integer> dice = List.of();

    DicePanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(220, 58));
        setMinimumSize(getPreferredSize());
    }

    void setDice(List<Integer> dice) {
        this.dice = dice == null ? List.of() : List.copyOf(dice);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int totalWidth = dice.size() * DIE_SIZE + Math.max(0, dice.size() - 1) * GAP;
            int x = Math.max(0, (getWidth() - totalWidth) / 2);
            int y = Math.max(0, (getHeight() - DIE_SIZE) / 2);
            for (int die : dice) {
                paintDie(g, x, y, die);
                x += DIE_SIZE + GAP;
            }
        } finally {
            g.dispose();
        }
    }

    private void paintDie(Graphics2D g, int x, int y, int value) {
        RoundRectangle2D shape = new RoundRectangle2D.Double(x, y, DIE_SIZE, DIE_SIZE, 12, 12);
        g.setColor(Theme.WHITE_CHECKER);
        g.fill(shape);
        g.setColor(new Color(0, 0, 0, 70));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(shape);

        boolean left = value == 2 || value == 3 || value == 4 || value == 5 || value == 6;
        boolean right = left;
        boolean center = value == 1 || value == 3 || value == 5;
        boolean middlePair = value == 6;
        int low = 12;
        int mid = DIE_SIZE / 2;
        int high = DIE_SIZE - 12;

        g.setColor(Theme.BLACK_CHECKER);
        if (left) {
            pip(g, x + low, y + low);
            pip(g, x + high, y + high);
        }
        if (right && value >= 4) {
            pip(g, x + high, y + low);
            pip(g, x + low, y + high);
        }
        if (center) {
            pip(g, x + mid, y + mid);
        }
        if (middlePair) {
            pip(g, x + low, y + mid);
            pip(g, x + high, y + mid);
        }
    }

    private void pip(Graphics2D g, int centerX, int centerY) {
        int radius = 4;
        g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
}
