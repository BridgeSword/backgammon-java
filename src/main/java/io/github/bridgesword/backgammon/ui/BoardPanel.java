package io.github.bridgesword.backgammon.ui;

import io.github.bridgesword.backgammon.model.Board;
import io.github.bridgesword.backgammon.model.Move;
import io.github.bridgesword.backgammon.model.Player;
import io.github.bridgesword.backgammon.model.Point;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Custom-painted Backgammon board. The component receives immutable state and
 * reports selected move candidates; it does not mutate the game itself.
 */
final class BoardPanel extends JComponent {
    private static final int OUTER_MARGIN = 18;
    private static final int FRAME_THICKNESS = 18;
    private static final int BAR_WIDTH = 48;
    private static final int OFF_TRAY_WIDTH = 38;

    private Board board = Board.initial();
    private Player activePlayer = Player.WHITE;
    private List<Move> legalMoves = List.of();
    private Point selected;
    private Point hovered;
    private boolean interactive = true;
    private Consumer<List<Move>> moveHandler = ignored -> { };

    BoardPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(860, 680));
        setMinimumSize(new Dimension(650, 500));
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Backgammon board");
        getAccessibleContext().setAccessibleDescription(
                "Select a highlighted checker and then a highlighted destination.");

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                Point candidate = locationAt(event.getPoint());
                hovered = isClickable(candidate) ? candidate : null;
                setCursor(hovered == null
                        ? Cursor.getDefaultCursor()
                        : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = null;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                handleClick(locationAt(event.getPoint()));
            }
        };
        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);
    }

    void setPosition(Board board, Player activePlayer, List<Move> legalMoves, boolean interactive) {
        this.board = Objects.requireNonNull(board, "board");
        this.activePlayer = Objects.requireNonNull(activePlayer, "activePlayer");
        this.legalMoves = List.copyOf(Objects.requireNonNull(legalMoves, "legalMoves"));
        this.interactive = interactive;
        if (selected != null && this.legalMoves.stream().noneMatch(move -> move.from().equals(selected))) {
            selected = null;
        }
        repaint();
    }

    void clearSelection() {
        selected = null;
        repaint();
    }

    void setMoveHandler(Consumer<List<Move>> moveHandler) {
        this.moveHandler = Objects.requireNonNull(moveHandler, "moveHandler");
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleBoardPanel();
        }
        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Geometry geometry = geometry();
            paintShadow(g, geometry);
            paintBoard(g, geometry);
            paintPoints(g, geometry);
            paintBar(g, geometry);
            paintOffTray(g, geometry);
            paintHighlights(g, geometry);
            paintCheckers(g, geometry);
            paintPointLabels(g, geometry);
        } finally {
            g.dispose();
        }
    }

    private void paintShadow(Graphics2D g, Geometry geometry) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(
                geometry.outer.x + 7,
                geometry.outer.y + 9,
                geometry.outer.width,
                geometry.outer.height,
                24,
                24);
    }

    private void paintBoard(Graphics2D g, Geometry geometry) {
        g.setPaint(new GradientPaint(
                geometry.outer.x,
                geometry.outer.y,
                new Color(0xA8734A),
                geometry.outer.x,
                geometry.outer.y + geometry.outer.height,
                new Color(0x6E452E)));
        g.fillRoundRect(
                geometry.outer.x,
                geometry.outer.y,
                geometry.outer.width,
                geometry.outer.height,
                22,
                22);
        g.setColor(new Color(255, 255, 255, 35));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(
                geometry.outer.x + 1,
                geometry.outer.y + 1,
                geometry.outer.width - 2,
                geometry.outer.height - 2,
                21,
                21);

        g.setPaint(new GradientPaint(
                geometry.playArea.x,
                geometry.playArea.y,
                new Color(0xE3C89C),
                geometry.playArea.x,
                geometry.playArea.y + geometry.playArea.height,
                Theme.BOARD_INNER));
        g.fillRect(
                geometry.playArea.x,
                geometry.playArea.y,
                geometry.playArea.width,
                geometry.playArea.height);

        g.setColor(new Color(0, 0, 0, 30));
        g.drawLine(
                geometry.playArea.x,
                geometry.midY,
                geometry.playArea.x + geometry.playArea.width,
                geometry.midY);
    }

    private void paintPoints(Graphics2D g, Geometry geometry) {
        for (int pointNumber = 1; pointNumber <= Board.POINT_COUNT; pointNumber++) {
            Polygon point = geometry.pointPolygon(pointNumber);
            boolean light = geometry.visualColumn(pointNumber) % 2 == 0;
            g.setColor(light ? Theme.POINT_LIGHT : Theme.POINT_DARK);
            g.fillPolygon(point);
            g.setColor(new Color(255, 255, 255, 22));
            g.drawPolygon(point);
        }
    }

    private void paintBar(Graphics2D g, Geometry geometry) {
        g.setPaint(new GradientPaint(
                geometry.bar.x,
                geometry.bar.y,
                new Color(0x765039),
                geometry.bar.x + geometry.bar.width,
                geometry.bar.y,
                new Color(0x9B6B48)));
        g.fillRect(geometry.bar.x, geometry.bar.y, geometry.bar.width, geometry.bar.height);
        g.setColor(new Color(255, 255, 255, 35));
        g.drawLine(geometry.bar.x + 3, geometry.bar.y, geometry.bar.x + 3,
                geometry.bar.y + geometry.bar.height);
        g.drawLine(geometry.bar.x + geometry.bar.width - 4, geometry.bar.y,
                geometry.bar.x + geometry.bar.width - 4, geometry.bar.y + geometry.bar.height);

        Graphics2D labelGraphics = (Graphics2D) g.create();
        try {
            labelGraphics.setFont(Theme.SMALL_FONT.deriveFont(java.awt.Font.BOLD));
            labelGraphics.setColor(new Color(255, 255, 255, 95));
            String label = "BAR";
            FontMetrics metrics = labelGraphics.getFontMetrics();
            labelGraphics.rotate(-Math.PI / 2,
                    geometry.bar.getCenterX(), geometry.bar.getCenterY());
            labelGraphics.drawString(label,
                    (int) geometry.bar.getCenterX() - metrics.stringWidth(label) / 2,
                    (int) geometry.bar.getCenterY() + metrics.getAscent() / 2);
        } finally {
            labelGraphics.dispose();
        }
    }

    private void paintOffTray(Graphics2D g, Geometry geometry) {
        g.setColor(new Color(0x5A3928));
        g.fillRoundRect(
                geometry.offTray.x,
                geometry.offTray.y,
                geometry.offTray.width,
                geometry.offTray.height,
                12,
                12);
        g.setColor(new Color(255, 255, 255, 30));
        g.drawRoundRect(
                geometry.offTray.x,
                geometry.offTray.y,
                geometry.offTray.width,
                geometry.offTray.height,
                12,
                12);

        paintOffCheckers(g, geometry, Player.BLACK, true);
        paintOffCheckers(g, geometry, Player.WHITE, false);
    }

    private void paintOffCheckers(Graphics2D g, Geometry geometry, Player player, boolean top) {
        int count = board.borneOff(player);
        if (count == 0) {
            return;
        }
        int visible = Math.min(count, 15);
        int halfHeight = geometry.offTray.height / 2 - 12;
        int checkerHeight = Math.max(4, Math.min(9, (halfHeight - 8) / 15));
        int x = geometry.offTray.x + 6;
        int width = geometry.offTray.width - 12;
        int startY = top
                ? geometry.offTray.y + 8
                : geometry.offTray.y + geometry.offTray.height - 8 - checkerHeight;
        for (int i = 0; i < visible; i++) {
            int y = top ? startY + i * checkerHeight : startY - i * checkerHeight;
            paintOffChecker(g, x, y, width, checkerHeight - 1, player);
        }
    }

    private void paintOffChecker(Graphics2D g, int x, int y, int width, int height, Player player) {
        g.setColor(player == Player.WHITE ? Theme.WHITE_CHECKER : Theme.BLACK_CHECKER);
        g.fillRoundRect(x, y, width, height, 5, 5);
        g.setColor(player == Player.WHITE ? new Color(0x9A8066) : new Color(0x8FA2A8));
        g.drawRoundRect(x, y, width, height, 5, 5);
    }

    private void paintHighlights(Graphics2D g, Geometry geometry) {
        Set<Point> sources = legalSources();
        if (interactive) {
            g.setStroke(new BasicStroke(3f));
            for (Point source : sources) {
                if (source.isBoard()) {
                    Polygon polygon = geometry.pointPolygon(source.number());
                    g.setColor(new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), 80));
                    g.drawPolygon(polygon);
                } else if (source.equals(Point.BAR)) {
                    g.setColor(new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), 80));
                    g.drawRect(geometry.bar.x + 3, geometry.bar.y + 3,
                            geometry.bar.width - 6, geometry.bar.height - 6);
                }
            }
        }

        if (selected != null) {
            paintLocationOutline(g, geometry, selected, Theme.GOLD, 4f);
            for (Point destination : legalDestinations(selected)) {
                if (destination.isBoard()) {
                    Polygon polygon = geometry.pointPolygon(destination.number());
                    g.setColor(new Color(
                            Theme.SUCCESS.getRed(), Theme.SUCCESS.getGreen(), Theme.SUCCESS.getBlue(), 110));
                    g.fillPolygon(polygon);
                    g.setColor(Theme.SUCCESS);
                    g.setStroke(new BasicStroke(3f));
                    g.drawPolygon(polygon);
                } else if (destination.equals(Point.OFF)) {
                    g.setColor(new Color(
                            Theme.SUCCESS.getRed(), Theme.SUCCESS.getGreen(), Theme.SUCCESS.getBlue(), 100));
                    g.fillRoundRect(geometry.offTray.x, geometry.offTray.y,
                            geometry.offTray.width, geometry.offTray.height, 12, 12);
                    g.setColor(Theme.SUCCESS);
                    g.setStroke(new BasicStroke(3f));
                    g.drawRoundRect(geometry.offTray.x, geometry.offTray.y,
                            geometry.offTray.width, geometry.offTray.height, 12, 12);
                }
            }
        }

        if (hovered != null) {
            paintLocationOutline(g, geometry, hovered, new Color(255, 255, 255, 150), 2f);
        }
    }

    private void paintLocationOutline(
            Graphics2D g, Geometry geometry, Point location, Color color, float strokeWidth) {
        g.setColor(color);
        g.setStroke(new BasicStroke(strokeWidth));
        if (location.isBoard()) {
            g.drawPolygon(geometry.pointPolygon(location.number()));
        } else if (location.equals(Point.BAR)) {
            g.drawRect(geometry.bar.x + 3, geometry.bar.y + 3,
                    geometry.bar.width - 6, geometry.bar.height - 6);
        } else if (location.equals(Point.OFF)) {
            g.drawRoundRect(geometry.offTray.x, geometry.offTray.y,
                    geometry.offTray.width, geometry.offTray.height, 12, 12);
        }
    }

    private void paintCheckers(Graphics2D g, Geometry geometry) {
        for (int pointNumber = 1; pointNumber <= Board.POINT_COUNT; pointNumber++) {
            int currentPoint = pointNumber;
            board.ownerAt(currentPoint).ifPresent(player ->
                    paintPointStack(g, geometry, currentPoint, player, board.countAt(currentPoint, player)));
        }
        paintBarStack(g, geometry, Player.BLACK, true);
        paintBarStack(g, geometry, Player.WHITE, false);
    }

    private void paintPointStack(
            Graphics2D g, Geometry geometry, int pointNumber, Player player, int count) {
        boolean top = geometry.isTop(pointNumber);
        int diameter = geometry.checkerDiameter;
        int centerX = geometry.pointCenterX(pointNumber);
        int step = Math.max(15, (int) (diameter * 0.82));
        int visible = Math.min(count, 5);
        for (int i = 0; i < visible; i++) {
            int centerY = top
                    ? geometry.playArea.y + diameter / 2 + 5 + i * step
                    : geometry.playArea.y + geometry.playArea.height - diameter / 2 - 5 - i * step;
            boolean showCount = i == visible - 1 && count > visible;
            paintChecker(g, centerX, centerY, diameter, player, showCount ? count : 0);
        }
    }

    private void paintBarStack(Graphics2D g, Geometry geometry, Player player, boolean top) {
        int count = board.bar(player);
        int diameter = Math.min(geometry.checkerDiameter, geometry.bar.width - 10);
        int step = Math.max(14, (int) (diameter * 0.75));
        int visible = Math.min(count, 4);
        int centerX = (int) geometry.bar.getCenterX();
        for (int i = 0; i < visible; i++) {
            int centerY = top
                    ? geometry.midY - diameter / 2 - 10 - i * step
                    : geometry.midY + diameter / 2 + 10 + i * step;
            boolean showCount = i == visible - 1 && count > visible;
            paintChecker(g, centerX, centerY, diameter, player, showCount ? count : 0);
        }
    }

    private void paintChecker(
            Graphics2D g, int centerX, int centerY, int diameter, Player player, int countBadge) {
        int x = centerX - diameter / 2;
        int y = centerY - diameter / 2;
        g.setColor(new Color(0, 0, 0, 65));
        g.fillOval(x + 2, y + 3, diameter, diameter);

        Color center = player == Player.WHITE ? new Color(0xFFFDF8) : new Color(0x4B5B60);
        Color edge = player == Player.WHITE ? new Color(0xD7C8B5) : new Color(0x182125);
        float radius = diameter / 2f;
        g.setPaint(new RadialGradientPaint(
                centerX - radius * 0.25f,
                centerY - radius * 0.3f,
                radius,
                new float[]{0f, 1f},
                new Color[]{center, edge}));
        g.fill(new Ellipse2D.Double(x, y, diameter, diameter));
        g.setColor(player == Player.WHITE ? new Color(0x8D7B67) : new Color(0x9AABB0));
        g.setStroke(new BasicStroke(1.4f));
        g.drawOval(x, y, diameter, diameter);

        if (countBadge > 0) {
            String text = Integer.toString(countBadge);
            g.setFont(Theme.HEADING_FONT);
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(player == Player.WHITE ? Theme.BLACK_CHECKER : Theme.WHITE_CHECKER);
            g.drawString(text,
                    centerX - metrics.stringWidth(text) / 2,
                    centerY + (metrics.getAscent() - metrics.getDescent()) / 2);
        }
    }

    private void paintPointLabels(Graphics2D g, Geometry geometry) {
        g.setFont(Theme.SMALL_FONT.deriveFont(java.awt.Font.BOLD));
        FontMetrics metrics = g.getFontMetrics();
        for (int pointNumber = 1; pointNumber <= Board.POINT_COUNT; pointNumber++) {
            String label = Integer.toString(pointNumber);
            int x = geometry.pointCenterX(pointNumber) - metrics.stringWidth(label) / 2;
            int y = geometry.isTop(pointNumber)
                    ? geometry.midY - 7
                    : geometry.midY + metrics.getAscent() + 6;
            g.setColor(new Color(40, 35, 30, 150));
            g.drawString(label, x, y);
        }
    }

    private void handleClick(Point location) {
        if (!interactive || location == null) {
            return;
        }
        Set<Point> sources = legalSources();
        if (selected == null) {
            if (sources.contains(location)) {
                selected = location;
                repaint();
            }
            return;
        }

        if (sources.contains(location)) {
            selected = location;
            repaint();
            return;
        }

        List<Move> candidates = legalMoves.stream()
                .filter(move -> move.from().equals(selected) && move.to().equals(location))
                .toList();
        if (!candidates.isEmpty()) {
            selected = null;
            repaint();
            moveHandler.accept(candidates);
        }
    }

    private boolean isClickable(Point location) {
        if (!interactive || location == null) {
            return false;
        }
        if (legalSources().contains(location)) {
            return true;
        }
        return selected != null && legalDestinations(selected).contains(location);
    }

    private Set<Point> legalSources() {
        Set<Point> result = new LinkedHashSet<>();
        for (Move move : legalMoves) {
            result.add(move.from());
        }
        return result;
    }

    private Set<Point> legalDestinations(Point source) {
        Set<Point> result = new LinkedHashSet<>();
        for (Move move : legalMoves) {
            if (move.from().equals(source)) {
                result.add(move.to());
            }
        }
        return result;
    }

    private Point locationAt(java.awt.Point mouse) {
        Geometry geometry = geometry();
        if (geometry.offTray.contains(mouse)) {
            return Point.OFF;
        }
        if (geometry.bar.contains(mouse)) {
            return Point.BAR;
        }
        for (int pointNumber = 1; pointNumber <= Board.POINT_COUNT; pointNumber++) {
            if (geometry.pointPolygon(pointNumber).contains(mouse)) {
                return Point.board(pointNumber);
            }
        }
        return null;
    }

    private Geometry geometry() {
        int width = Math.max(getWidth(), getMinimumSize().width);
        int height = Math.max(getHeight(), getMinimumSize().height);
        return new Geometry(width, height);
    }

    private static final class Geometry {
        private final Rectangle outer;
        private final Rectangle playArea;
        private final Rectangle bar;
        private final Rectangle offTray;
        private final int midY;
        private final int pointWidth;
        private final int checkerDiameter;
        private final int leftStart;
        private final int rightStart;

        private Geometry(int width, int height) {
            outer = new Rectangle(
                    OUTER_MARGIN,
                    OUTER_MARGIN,
                    Math.max(1, width - OUTER_MARGIN * 2),
                    Math.max(1, height - OUTER_MARGIN * 2));
            int innerX = outer.x + FRAME_THICKNESS;
            int innerY = outer.y + FRAME_THICKNESS;
            int innerHeight = outer.height - FRAME_THICKNESS * 2;
            int trayX = outer.x + outer.width - FRAME_THICKNESS - OFF_TRAY_WIDTH;
            int boardRight = trayX - 10;
            int availableWidth = boardRight - innerX;
            pointWidth = Math.max(24, (availableWidth - BAR_WIDTH) / 12);
            int actualPlayWidth = pointWidth * 12 + BAR_WIDTH;
            playArea = new Rectangle(innerX, innerY, actualPlayWidth, innerHeight);
            leftStart = innerX;
            bar = new Rectangle(innerX + pointWidth * 6, innerY, BAR_WIDTH, innerHeight);
            rightStart = bar.x + BAR_WIDTH;
            offTray = new Rectangle(trayX, innerY, OFF_TRAY_WIDTH, innerHeight);
            midY = innerY + innerHeight / 2;
            checkerDiameter = Math.max(24, Math.min(42, pointWidth - 7));
        }

        private Polygon pointPolygon(int pointNumber) {
            int center = pointCenterX(pointNumber);
            int half = Math.max(8, pointWidth / 2 - 1);
            int depth = Math.max(120, playArea.height / 2 - 30);
            if (isTop(pointNumber)) {
                return new Polygon(
                        new int[]{center - half, center + half, center},
                        new int[]{playArea.y, playArea.y, playArea.y + depth},
                        3);
            }
            int bottom = playArea.y + playArea.height;
            return new Polygon(
                    new int[]{center - half, center + half, center},
                    new int[]{bottom, bottom, bottom - depth},
                    3);
        }

        private int pointCenterX(int pointNumber) {
            int column = visualColumn(pointNumber);
            return (column < 6 ? leftStart : rightStart) + (column % 6) * pointWidth + pointWidth / 2;
        }

        private int visualColumn(int pointNumber) {
            if (pointNumber >= 13) {
                return pointNumber - 13;
            }
            return 12 - pointNumber;
        }

        private boolean isTop(int pointNumber) {
            return pointNumber >= 13;
        }
    }

    private final class AccessibleBoardPanel extends AccessibleJComponent {
        private static final long serialVersionUID = 1L;
    }
}
