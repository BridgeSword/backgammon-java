package io.github.bridgesword.backgammon.ui;

import io.github.bridgesword.backgammon.ai.BackgammonAi;
import io.github.bridgesword.backgammon.ai.Difficulty;
import io.github.bridgesword.backgammon.engine.GameSession;
import io.github.bridgesword.backgammon.model.GamePhase;
import io.github.bridgesword.backgammon.model.GameSnapshot;
import io.github.bridgesword.backgammon.model.Move;
import io.github.bridgesword.backgammon.model.MoveSequence;
import io.github.bridgesword.backgammon.model.Player;
import io.github.bridgesword.backgammon.model.TurnOutcome;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Main game view and Swing-side turn coordinator. */
final class GamePanel extends JPanel {
    private static final int AI_ROLL_DELAY_MS = 500;
    private static final int AI_MOVE_DELAY_MS = 420;

    private final BoardPanel boardPanel = new BoardPanel();
    private final DicePanel dicePanel = new DicePanel();
    private final JComboBox<GameMode> modeBox = new JComboBox<>(GameMode.values());
    private final JComboBox<SideChoice> sideBox = new JComboBox<>(SideChoice.values());
    private final JComboBox<Difficulty> difficultyBox = new JComboBox<>(Difficulty.values());
    private final JButton newGameButton = actionButton("New game", true);
    private final JButton rollButton = actionButton("Roll dice", true);
    private final JButton undoButton = actionButton("Undo", false);
    private final JButton rulesButton = actionButton("Rules", false);
    private final JLabel turnLabel = new JLabel();
    private final JTextArea statusText = new JTextArea(2, 22);
    private final JLabel whitePipLabel = statValue();
    private final JLabel blackPipLabel = statValue();
    private final JLabel whiteBarLabel = statValue();
    private final JLabel blackBarLabel = statValue();
    private final JLabel whiteOffLabel = statValue();
    private final JLabel blackOffLabel = statValue();
    private final JLabel humanSideLabel = new JLabel();
    private final JProgressBar thinkingBar = new JProgressBar();
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);

    private final Random gameRandom;
    private final BackgammonAi computer;
    private final boolean automationEnabled;

    private GameSession session;
    private Player humanPlayer = Player.WHITE;
    private boolean aiBusy;
    private boolean winnerShown;
    private int sessionGeneration;

    GamePanel() {
        this(true, new Random(), new Random());
    }

    /** Creates deterministic, non-animating content for the README preview. */
    static GamePanel preview() {
        return new GamePanel(false, new Random(2), new Random(17));
    }

    private GamePanel(boolean automationEnabled, Random gameRandom, Random aiRandom) {
        super(new BorderLayout(18, 18));
        this.automationEnabled = automationEnabled;
        this.gameRandom = gameRandom;
        this.computer = new BackgammonAi(aiRandom);

        setBackground(Theme.APP_BACKGROUND);
        setBorder(new EmptyBorder(18, 22, 20, 22));
        add(createHeader(), BorderLayout.NORTH);
        add(createBoardCard(), BorderLayout.CENTER);
        add(createSidebarScroller(), BorderLayout.EAST);

        modeBox.setSelectedItem(GameMode.HUMAN_VS_AI);
        sideBox.setSelectedItem(SideChoice.WHITE);
        difficultyBox.setSelectedItem(Difficulty.MEDIUM);
        installListeners();
        startNewGame();
    }

    void startNewGame() {
        sessionGeneration++;
        aiBusy = false;
        winnerShown = false;
        humanPlayer = switch ((SideChoice) sideBox.getSelectedItem()) {
            case WHITE -> Player.WHITE;
            case BLACK -> Player.BLACK;
            case RANDOM -> gameRandom.nextBoolean() ? Player.WHITE : Player.BLACK;
        };
        session = new GameSession(gameRandom);
        historyModel.clear();
        appendHistory("New game");
        appendHistory(session.snapshot().lastEvent().message());
        boardPanel.clearSelection();
        refresh();
        scheduleComputerIfNeeded();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("BACKGAMMON");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE_FONT);
        JLabel subtitle = new JLabel("Rule-complete Java engine · Local multiplayer · Game AI");
        subtitle.setForeground(Theme.MUTED_TEXT);
        subtitle.setFont(Theme.BODY_FONT);
        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);

        JLabel javaBadge = new JLabel(" JAVA 17 ");
        javaBadge.setOpaque(true);
        javaBadge.setBackground(Theme.POINT_DARK);
        javaBadge.setForeground(Theme.TEXT);
        javaBadge.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD));
        javaBadge.setBorder(new EmptyBorder(7, 10, 7, 10));
        header.add(javaBadge, BorderLayout.EAST);
        return header;
    }

    private JPanel createBoardCard() {
        RoundedPanel card = new RoundedPanel(Theme.PANEL, 18);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.add(boardPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(300, 760));

        sidebar.add(createSetupCard());
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(createTurnCard());
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(createPositionCard());
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(createHistoryCard());
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(createFooterActions());
        return sidebar;
    }

    private JScrollPane createSidebarScroller() {
        JScrollPane scrollPane = new JScrollPane(
                createSidebar(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(null);
        scrollPane.setBackground(Theme.APP_BACKGROUND);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(Theme.APP_BACKGROUND);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setFocusable(false);
        scrollPane.getViewport().setFocusable(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setPreferredSize(new Dimension(306, 0));
        return scrollPane;
    }

    private JPanel createSetupCard() {
        RoundedPanel card = card("GAME SETUP");
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.setOpaque(false);
        form.add(formRow("Mode", modeBox));
        form.add(formRow("Play as", sideBox));
        form.add(formRow("AI level", difficultyBox));
        card.add(form, BorderLayout.CENTER);
        JPanel action = new JPanel(new BorderLayout());
        action.setOpaque(false);
        action.setBorder(new EmptyBorder(10, 0, 0, 0));
        action.add(newGameButton);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createTurnCard() {
        RoundedPanel card = card("CURRENT TURN");
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        turnLabel.setFont(Theme.HEADING_FONT.deriveFont(18f));
        turnLabel.setForeground(Theme.TEXT);
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusText.setFont(Theme.BODY_FONT);
        statusText.setForeground(Theme.MUTED_TEXT);
        statusText.setOpaque(true);
        statusText.setBackground(Theme.PANEL);
        statusText.setEditable(false);
        statusText.setFocusable(false);
        statusText.setLineWrap(true);
        statusText.setWrapStyleWord(true);
        statusText.setBorder(BorderFactory.createEmptyBorder());
        statusText.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusText.setPreferredSize(new Dimension(250, 42));
        statusText.setMinimumSize(new Dimension(220, 38));
        statusText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        humanSideLabel.setFont(Theme.SMALL_FONT);
        humanSideLabel.setForeground(Theme.GOLD);
        humanSideLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        thinkingBar.setIndeterminate(true);
        thinkingBar.setVisible(false);
        thinkingBar.setBorderPainted(false);
        thinkingBar.setForeground(Theme.GOLD);
        thinkingBar.setBackground(Theme.PANEL_ALT);
        thinkingBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        thinkingBar.setPreferredSize(new Dimension(240, 4));

        dicePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rollButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        rollButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        content.add(turnLabel);
        content.add(Box.createVerticalStrut(3));
        content.add(humanSideLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(statusText);
        content.add(Box.createVerticalStrut(8));
        content.add(thinkingBar);
        content.add(Box.createVerticalStrut(5));
        content.add(dicePanel);
        content.add(Box.createVerticalStrut(4));
        content.add(rollButton);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPositionCard() {
        RoundedPanel card = card("POSITION");
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 5, 3, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;

        addStatHeader(grid, constraints, 1, "WHITE", Theme.WHITE_CHECKER);
        addStatHeader(grid, constraints, 2, "BLACK", new Color(0x90A4AE));
        addStatRow(grid, constraints, 1, "Pip count", whitePipLabel, blackPipLabel);
        addStatRow(grid, constraints, 2, "On bar", whiteBarLabel, blackBarLabel);
        addStatRow(grid, constraints, 3, "Borne off", whiteOffLabel, blackOffLabel);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createHistoryCard() {
        RoundedPanel card = card("TURN HISTORY");
        historyList.setBackground(Theme.PANEL);
        historyList.setForeground(Theme.MUTED_TEXT);
        historyList.setFont(Theme.SMALL_FONT);
        historyList.setSelectionBackground(Theme.PANEL_ALT);
        historyList.setSelectionForeground(Theme.TEXT);
        historyList.setFocusable(false);
        historyList.setCellRenderer(new HistoryRenderer());
        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.PANEL);
        scrollPane.setPreferredSize(new Dimension(260, 105));
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFooterActions() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setOpaque(false);
        panel.add(undoButton);
        panel.add(rulesButton);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return panel;
    }

    private RoundedPanel card(String titleText) {
        RoundedPanel card = new RoundedPanel(Theme.PANEL, 16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        JLabel title = new JLabel(titleText);
        title.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD));
        title.setForeground(Theme.GOLD);
        card.add(title, BorderLayout.NORTH);
        return card;
    }

    private JPanel formRow(String labelText, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(Theme.MUTED_TEXT);
        label.setFont(Theme.SMALL_FONT);
        label.setPreferredSize(new Dimension(62, 28));
        component.setFont(Theme.BODY_FONT);
        component.setPreferredSize(new Dimension(150, 28));
        row.add(label, BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        return row;
    }

    private void addStatHeader(
            JPanel grid, GridBagConstraints constraints, int column, String text, Color color) {
        constraints.gridx = column;
        constraints.gridy = 0;
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD));
        label.setForeground(color);
        grid.add(label, constraints);
    }

    private void addStatRow(
            JPanel grid,
            GridBagConstraints constraints,
            int row,
            String name,
            JLabel white,
            JLabel black) {
        constraints.gridy = row;
        constraints.gridx = 0;
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(Theme.SMALL_FONT);
        nameLabel.setForeground(Theme.MUTED_TEXT);
        grid.add(nameLabel, constraints);
        constraints.gridx = 1;
        grid.add(white, constraints);
        constraints.gridx = 2;
        grid.add(black, constraints);
    }

    private void installListeners() {
        boardPanel.setMoveHandler(this::handleHumanMove);
        newGameButton.addActionListener(event -> startNewGame());
        rollButton.addActionListener(event -> rollForCurrentPlayer());
        undoButton.addActionListener(event -> undo());
        rulesButton.addActionListener(event -> showRules());
        modeBox.addActionListener(event -> startNewGame());
        sideBox.addActionListener(event -> {
            if (selectedMode() == GameMode.HUMAN_VS_AI) {
                startNewGame();
            }
        });
        difficultyBox.addActionListener(event -> refresh());
    }

    private void handleHumanMove(List<Move> candidates) {
        if (aiBusy || !isHumanControlled(session.currentPlayer()) || candidates.isEmpty()) {
            return;
        }
        Move move = candidates.size() == 1 ? candidates.get(0) : chooseDie(candidates);
        if (move == null) {
            return;
        }
        applyAndDisplayMove(move);
        scheduleComputerIfNeeded();
    }

    private Move chooseDie(List<Move> candidates) {
        Object selection = JOptionPane.showInputDialog(
                this,
                "Both dice reach that destination. Which die should be used?",
                "Choose a die",
                JOptionPane.QUESTION_MESSAGE,
                null,
                candidates.stream().map(move -> "Die " + move.die()).toArray(),
                "Die " + candidates.get(0).die());
        if (selection == null) {
            return null;
        }
        int die = Integer.parseInt(selection.toString().substring(4));
        return candidates.stream().filter(candidate -> candidate.die() == die).findFirst().orElseThrow();
    }

    private void rollForCurrentPlayer() {
        if (aiBusy || session.phase() != GamePhase.WAITING_FOR_ROLL
                || !isHumanControlled(session.currentPlayer())) {
            return;
        }
        GameSnapshot after = session.roll();
        appendHistory(after.lastEvent().message());
        refresh();
        scheduleComputerIfNeeded();
    }

    private void applyAndDisplayMove(Move move) {
        appendHistory(formatMove(move));
        GameSnapshot after = session.move(move);
        boardPanel.clearSelection();
        if (after.lastEvent().outcome() != TurnOutcome.MOVE_APPLIED) {
            appendHistory(after.lastEvent().message());
        }
        refresh();
        showWinnerIfNeeded();
    }

    private void undo() {
        if (aiBusy || !canUseUndo(session.snapshot())) {
            return;
        }
        GameSnapshot snapshot = session.undo();
        appendHistory("↶ " + snapshot.lastEvent().message());
        winnerShown = false;
        boardPanel.clearSelection();
        refresh();
    }

    private void scheduleComputerIfNeeded() {
        if (!automationEnabled || aiBusy || session == null) {
            return;
        }
        GameSnapshot snapshot = session.snapshot();
        if (snapshot.phase() == GamePhase.GAME_OVER || !isComputerControlled(snapshot.currentPlayer())) {
            return;
        }
        aiBusy = true;
        refresh();
        int generation = sessionGeneration;

        if (snapshot.phase() == GamePhase.WAITING_FOR_ROLL) {
            Timer timer = new Timer(AI_ROLL_DELAY_MS, event -> {
                ((Timer) event.getSource()).stop();
                if (generation != sessionGeneration) {
                    return;
                }
                GameSnapshot afterRoll = session.roll();
                appendHistory(afterRoll.lastEvent().message());
                refresh();
                if (afterRoll.phase() != GamePhase.IN_TURN
                        || !isComputerControlled(afterRoll.currentPlayer())) {
                    aiBusy = false;
                    refresh();
                    return;
                }
                chooseComputerTurn(generation);
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            chooseComputerTurn(generation);
        }
    }

    private void chooseComputerTurn(int generation) {
        GameSnapshot snapshot = session.snapshot();
        Difficulty difficulty = selectedDifficulty();
        SwingWorker<Optional<MoveSequence>, Void> worker = new SwingWorker<>() {
            @Override
            protected Optional<MoveSequence> doInBackground() {
                return computer.chooseSequence(
                        snapshot.board(),
                        snapshot.currentPlayer(),
                        snapshot.remainingDice(),
                        difficulty);
            }

            @Override
            protected void done() {
                if (generation != sessionGeneration) {
                    return;
                }
                try {
                    Optional<MoveSequence> choice = get();
                    if (choice.isEmpty()) {
                        aiBusy = false;
                        refresh();
                    } else {
                        animateComputerMoves(choice.orElseThrow().moves(), generation);
                    }
                } catch (Exception exception) {
                    aiBusy = false;
                    refresh();
                    showError("The computer player could not finish its turn.", exception);
                }
            }
        };
        worker.execute();
    }

    private void animateComputerMoves(List<Move> moves, int generation) {
        List<Move> queue = new ArrayList<>(moves);
        int[] index = {0};
        Timer timer = new Timer(AI_MOVE_DELAY_MS, null);
        timer.addActionListener(event -> {
            if (generation != sessionGeneration) {
                timer.stop();
                return;
            }
            if (index[0] >= queue.size()) {
                timer.stop();
                aiBusy = false;
                refresh();
                showWinnerIfNeeded();
                return;
            }
            Move move = queue.get(index[0]++);
            try {
                applyAndDisplayMove(move);
            } catch (RuntimeException exception) {
                timer.stop();
                aiBusy = false;
                refresh();
                showError("The selected AI move was rejected by the rules engine.", exception);
                return;
            }
            if (index[0] >= queue.size()) {
                timer.stop();
                aiBusy = false;
                refresh();
                showWinnerIfNeeded();
            }
        });
        timer.setInitialDelay(AI_MOVE_DELAY_MS);
        timer.start();
    }

    private void refresh() {
        if (session == null) {
            return;
        }
        GameSnapshot snapshot = session.snapshot();
        boolean humanMayMove = !aiBusy
                && snapshot.phase() == GamePhase.IN_TURN
                && isHumanControlled(snapshot.currentPlayer());
        List<Move> moves = humanMayMove ? session.legalMoves() : List.of();
        boardPanel.setPosition(snapshot.board(), snapshot.currentPlayer(), moves, humanMayMove);
        dicePanel.setDice(snapshot.remainingDice());

        String turnText = snapshot.phase() == GamePhase.GAME_OVER
                ? snapshot.result().winner().displayName() + " wins"
                : snapshot.currentPlayer().displayName() + " to "
                        + (snapshot.phase() == GamePhase.WAITING_FOR_ROLL ? "roll" : "move");
        turnLabel.setText(turnText);
        turnLabel.setForeground(snapshot.currentPlayer() == Player.WHITE
                ? Theme.WHITE_CHECKER
                : new Color(0xAFC3C8));

        String status = aiBusy
                ? snapshot.currentPlayer().displayName() + " AI is thinking…"
                : snapshot.lastEvent().message();
        statusText.setText(status);
        statusText.setCaretPosition(0);
        thinkingBar.setVisible(aiBusy);
        humanSideLabel.setText(selectedMode() == GameMode.HUMAN_VS_AI
                ? "You play " + humanPlayer.displayName() + " · AI: " + selectedDifficulty()
                : "Both sides are controlled locally");

        whitePipLabel.setText(Integer.toString(snapshot.board().pipCount(Player.WHITE)));
        blackPipLabel.setText(Integer.toString(snapshot.board().pipCount(Player.BLACK)));
        whiteBarLabel.setText(Integer.toString(snapshot.board().bar(Player.WHITE)));
        blackBarLabel.setText(Integer.toString(snapshot.board().bar(Player.BLACK)));
        whiteOffLabel.setText(snapshot.board().borneOff(Player.WHITE) + " / 15");
        blackOffLabel.setText(snapshot.board().borneOff(Player.BLACK) + " / 15");

        boolean canRoll = !aiBusy
                && snapshot.phase() == GamePhase.WAITING_FOR_ROLL
                && isHumanControlled(snapshot.currentPlayer());
        rollButton.setEnabled(canRoll);
        rollButton.setText(canRoll
                ? "Roll dice"
                : snapshot.phase() == GamePhase.IN_TURN ? "Select a checker" : "Roll dice");
        undoButton.setEnabled(!aiBusy && canUseUndo(snapshot));
        modeBox.setEnabled(!aiBusy);
        sideBox.setEnabled(!aiBusy && selectedMode() == GameMode.HUMAN_VS_AI);
        difficultyBox.setEnabled(!aiBusy && selectedMode() == GameMode.HUMAN_VS_AI);
        newGameButton.setEnabled(!aiBusy);
        revalidate();
        repaint();
    }

    private boolean canUseUndo(GameSnapshot snapshot) {
        return snapshot.canUndo()
                && (selectedMode() == GameMode.LOCAL_TWO_PLAYER
                || snapshot.lastEvent().player() == humanPlayer);
    }

    private boolean isHumanControlled(Player player) {
        return selectedMode() == GameMode.LOCAL_TWO_PLAYER || player == humanPlayer;
    }

    private boolean isComputerControlled(Player player) {
        return selectedMode() == GameMode.HUMAN_VS_AI && player != humanPlayer;
    }

    private GameMode selectedMode() {
        return (GameMode) modeBox.getSelectedItem();
    }

    private Difficulty selectedDifficulty() {
        return (Difficulty) difficultyBox.getSelectedItem();
    }

    private void appendHistory(String entry) {
        historyModel.addElement(entry);
        while (historyModel.size() > 80) {
            historyModel.remove(0);
        }
        int lastIndex = historyModel.size() - 1;
        if (lastIndex >= 0) {
            historyList.ensureIndexIsVisible(lastIndex);
        }
    }

    private void showWinnerIfNeeded() {
        GameSnapshot snapshot = session.snapshot();
        if (winnerShown || snapshot.phase() != GamePhase.GAME_OVER) {
            return;
        }
        winnerShown = true;
        String message = snapshot.result().winner().displayName() + " wins a "
                + snapshot.result().winType().name().toLowerCase() + " ("
                + snapshot.result().points() + " point"
                + (snapshot.result().points() == 1 ? "" : "s") + ").";
        JOptionPane.showMessageDialog(this, message, "Game over", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showRules() {
        String rules = "<html><body style='width:390px;font-family:sans-serif'>"
                + "<h2>How to play</h2>"
                + "<p>Move all 15 checkers into your home board, then bear them off. "
                + "White moves from point 24 toward 1; Black moves from 1 toward 24.</p>"
                + "<ul>"
                + "<li>Checkers on the bar must re-enter before any other checker moves.</li>"
                + "<li>A point with two or more opposing checkers is blocked.</li>"
                + "<li>Landing on a single opposing checker sends it to the bar.</li>"
                + "<li>Use both dice whenever possible. If only one can be used, use the higher.</li>"
                + "<li>Doubles are played four times.</li>"
                + "</ul>"
                + "<p>Highlighted checkers and destinations are always legal under the complete turn rules.</p>"
                + "</body></html>";
        JOptionPane.showMessageDialog(this, rules, "Backgammon rules", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message, Exception exception) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n\n" + exception.getMessage(),
                "Backgammon error",
                JOptionPane.ERROR_MESSAGE);
    }

    private static String formatMove(Move move) {
        return move.player().displayName() + "  " + move.from().displayName()
                + " → " + move.to().displayName() + "  [" + move.die() + "]";
    }

    private static JButton actionButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD));
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(9, 12, 9, 12));
        button.setBackground(primary ? Theme.POINT_DARK : Theme.PANEL_ALT);
        button.setForeground(Theme.TEXT);
        return button;
    }

    private static JLabel statValue() {
        JLabel label = new JLabel("0", SwingConstants.CENTER);
        label.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD));
        label.setForeground(Theme.TEXT);
        return label;
    }

    private enum SideChoice {
        WHITE("White"),
        BLACK("Black"),
        RANDOM("Random");

        private final String label;

        SideChoice(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class RoundedPanel extends JPanel {
        private final Color fill;
        private final int radius;

        private RoundedPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static final class HistoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focus);
            label.setBorder(new EmptyBorder(3, 2, 3, 2));
            label.setFont(Theme.SMALL_FONT);
            if (!selected) {
                label.setBackground(Theme.PANEL);
                label.setForeground(index == list.getModel().getSize() - 1
                        ? Theme.TEXT
                        : Theme.MUTED_TEXT);
            }
            return label;
        }
    }
}
