package view;

import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Path Memory Brain Training Game.
 * A numbered path is shown briefly on a grid.
 * Player must click the cells in the same order.
 *
 * Level system:
 *   Level 1:  3x3 grid, 3 steps, shown 2.5s
 *   Level 5:  5x5 grid, 7 steps, shown 1.5s
 *   Level 10: 7x7 grid, 14 steps, shown 0.8s
 */
public class PathMemoryView extends JPanel {

    public interface PathMemoryListener {
        void onBack();
        void onLevelComplete(int level, int score);
    }

    private PathMemoryListener listener;
    private int level;
    private int score = 0;
    private int gridSize;
    private int pathLength;
    private int previewMs;

    private List<int[]> correctPath = new ArrayList<int[]>();
    private List<int[]> playerPath  = new ArrayList<int[]>();

    private enum State { PREVIEW, PLAYER_TURN, WRONG, LEVEL_COMPLETE }
    private State state = State.PREVIEW;

    // Fixed UI containers
    private JPanel centerPanel;   // holds the grid, replaced each level
    private JButton[][] gridBtns;
    private JLabel statusLabel;
    private JLabel levelLabel;
    private JLabel scoreLabel;
    private JLabel stepLabel;

    private static final Color COLOR_EMPTY   = new Color(30, 45, 80);
    private static final Color COLOR_PATH    = new Color(52, 152, 219);
    private static final Color COLOR_PLAYER  = new Color(46, 204, 113);
    private static final Color COLOR_WRONG   = new Color(231, 76, 60);
    private static final Color COLOR_START   = new Color(241, 196, 15);

    public PathMemoryView(int startLevel, PathMemoryListener listener) {
        this.listener = listener;
        this.level    = startLevel;

        // Use BorderLayout for the entire view
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_DARK);

        buildHUD();
        buildStatusBar();

        // Create a fixed center panel that will hold the grid
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UIConstants.BG_DARK);
        add(centerPanel, BorderLayout.CENTER);

        startLevel(startLevel);
    }

    // ---- HUD ----
    private void buildHUD() {
        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(UIConstants.BG_PANEL);
        hud.setBorder(new EmptyBorder(10, 20, 10, 20));
        hud.setPreferredSize(new Dimension(0, 50));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        levelLabel = hudLabel("Level 1");
        scoreLabel = hudLabel("Score: 0");
        left.add(levelLabel);
        left.add(makeSep());
        left.add(scoreLabel);

        JLabel title = new JLabel("Path Memory", SwingConstants.CENTER);
        title.setFont(UIConstants.FONT_HEADING);
        title.setForeground(UIConstants.ACCENT_CYAN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JButton homeBtn = new JButton("Home");
        homeBtn.setFont(UIConstants.FONT_SMALL);
        homeBtn.setBackground(new Color(60, 60, 80));
        homeBtn.setForeground(UIConstants.TEXT_PRIMARY);
        homeBtn.setOpaque(true);
        homeBtn.setFocusPainted(false);
        homeBtn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        homeBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onBack(); }
        });
        right.add(homeBtn);

        hud.add(left,  BorderLayout.WEST);
        hud.add(title, BorderLayout.CENTER);
        hud.add(right, BorderLayout.EAST);
        add(hud, BorderLayout.NORTH);
    }

    private void buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 6));
        bar.setBackground(UIConstants.BG_DARK);
        bar.setPreferredSize(new Dimension(0, 40));

        statusLabel = new JLabel("Watch the path carefully...");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        statusLabel.setForeground(UIConstants.ACCENT_CYAN);

        stepLabel = new JLabel("");
        stepLabel.setFont(UIConstants.FONT_SMALL);
        stepLabel.setForeground(UIConstants.TEXT_MUTED);

        bar.add(statusLabel);
        bar.add(stepLabel);
        add(bar, BorderLayout.SOUTH);
    }

    // ---- Level Setup ----
    private void startLevel(int lvl) {
        this.level      = lvl;
        this.gridSize   = Math.min(3 + (lvl - 1) / 2, 7);
        this.pathLength = Math.min(3 + lvl, (gridSize * gridSize) / 2);
        this.previewMs  = Math.max(800, 2500 - (lvl - 1) * 180);

        levelLabel.setText("Level " + lvl);
        scoreLabel.setText("Score: " + score);

        correctPath.clear();
        playerPath.clear();
        state = State.PREVIEW;

        generatePath();
        buildGrid();
        startPreview();
    }

    private void generatePath() {
        Random rnd = new Random();
        boolean[][] vis = new boolean[gridSize][gridSize];
        int r = rnd.nextInt(gridSize);
        int c = rnd.nextInt(gridSize);
        correctPath.add(new int[]{r, c});
        vis[r][c] = true;

        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int step = 1; step < pathLength; step++) {
            List<int[]> moves = new ArrayList<int[]>();
            int[] last = correctPath.get(correctPath.size() - 1);
            for (int[] d : dirs) {
                int nr = last[0] + d[0];
                int nc = last[1] + d[1];
                if (nr >= 0 && nr < gridSize && nc >= 0 && nc < gridSize && !vis[nr][nc])
                    moves.add(new int[]{nr, nc});
            }
            if (moves.isEmpty()) break;
            int[] next = moves.get(rnd.nextInt(moves.size()));
            correctPath.add(next);
            vis[next[0]][next[1]] = true;
        }
    }

    private void buildGrid() {
        // Clear the center panel completely
        centerPanel.removeAll();

        // Calculate cell size based on available space
        int availW = UIConstants.WINDOW_WIDTH  - 40;
        int availH = UIConstants.WINDOW_HEIGHT - 120;
        int cellW  = (availW - gridSize * 6) / gridSize;
        int cellH  = (availH - gridSize * 6) / gridSize;
        int cellSize = Math.max(55, Math.min(Math.min(cellW, cellH), 100));

        // Build grid with exact GridLayout
        JPanel grid = new JPanel(new GridLayout(gridSize, gridSize, 6, 6));
        grid.setBackground(UIConstants.BG_DARK);

        // Fix grid to exact pixel size so it stays square
        int totalW = cellSize * gridSize + 6 * (gridSize - 1);
        int totalH = cellSize * gridSize + 6 * (gridSize - 1);
        grid.setPreferredSize(new Dimension(totalW, totalH));
        grid.setMaximumSize(new Dimension(totalW, totalH));
        grid.setMinimumSize(new Dimension(totalW, totalH));

        gridBtns = new JButton[gridSize][gridSize];

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                JButton btn = new JButton();
                btn.setBackground(COLOR_EMPTY);
                btn.setForeground(Color.WHITE);
                btn.setOpaque(true);
                btn.setContentAreaFilled(true);
                btn.setFocusPainted(false);
                btn.setBorderPainted(true);
                btn.setBorder(BorderFactory.createLineBorder(new Color(50, 70, 120), 1));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.setFont(new Font("Segoe UI", Font.BOLD, cellSize / 3));
                btn.setPreferredSize(new Dimension(cellSize, cellSize));

                final int row = r, col = c;
                btn.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) {
                        onCellClicked(row, col);
                    }
                });

                gridBtns[r][c] = btn;
                grid.add(btn);
            }
        }

        // Wrap in GridBagLayout to center the fixed-size grid
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UIConstants.BG_DARK);
        wrapper.add(grid, new GridBagConstraints());

        centerPanel.add(wrapper, BorderLayout.CENTER);
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    // ---- Preview ----
    private void startPreview() {
        statusLabel.setText("Memorize the path!");
        statusLabel.setForeground(UIConstants.ACCENT_CYAN);
        stepLabel.setText("Showing for " + (previewMs / 1000.0) + "s");
        resetAllCells();
        showPathAnimated(0);
    }

    private void resetAllCells() {
        for (int r = 0; r < gridSize; r++)
            for (int c = 0; c < gridSize; c++) {
                gridBtns[r][c].setBackground(COLOR_EMPTY);
                gridBtns[r][c].setText("");
            }
    }

    private void showPathAnimated(final int stepIdx) {
        if (stepIdx >= correctPath.size()) {
            // All shown — hide after preview time
            Timer hide = new Timer(previewMs, new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    resetAllCells();
                    enablePlayerTurn();
                }
            });
            hide.setRepeats(false);
            hide.start();
            return;
        }

        int[] pos = correctPath.get(stepIdx);
        Color c = (stepIdx == 0) ? COLOR_START : COLOR_PATH;
        gridBtns[pos[0]][pos[1]].setBackground(c);
        gridBtns[pos[0]][pos[1]].setText(String.valueOf(stepIdx + 1));

        Timer next = new Timer(280, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                showPathAnimated(stepIdx + 1);
            }
        });
        next.setRepeats(false);
        next.start();
    }

    private void enablePlayerTurn() {
        state = State.PLAYER_TURN;
        playerPath.clear();
        statusLabel.setText("Now recreate the path!");
        statusLabel.setForeground(UIConstants.SUCCESS_GREEN);
        stepLabel.setText("Step 0 / " + correctPath.size());
    }

    // ---- Player Input ----
    private void onCellClicked(int row, int col) {
        if (state != State.PLAYER_TURN) return;

        int step = playerPath.size();
        if (step >= correctPath.size()) return;

        int[] expected = correctPath.get(step);

        if (row == expected[0] && col == expected[1]) {
            // Correct
            playerPath.add(new int[]{row, col});
            Color c = (step == 0) ? COLOR_START : COLOR_PLAYER;
            gridBtns[row][col].setBackground(c);
            gridBtns[row][col].setText(String.valueOf(step + 1));
            stepLabel.setText("Step " + playerPath.size() + " / " + correctPath.size());

            if (playerPath.size() == correctPath.size()) {
                state = State.LEVEL_COMPLETE;
                score += 100 + (level * 10);
                scoreLabel.setText("Score: " + score);
                statusLabel.setText("Perfect! Level Complete!");
                statusLabel.setForeground(UIConstants.SUCCESS_GREEN);
                listener.onLevelComplete(level, score);
                showLevelCompleteDialog();
            }
        } else {
            // Wrong
            state = State.WRONG;
            gridBtns[row][col].setBackground(COLOR_WRONG);
            statusLabel.setText("Wrong! Watch the correct path...");
            statusLabel.setForeground(UIConstants.ERROR_RED);

            Timer show = new Timer(600, new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    revealCorrectPath();
                }
            });
            show.setRepeats(false);
            show.start();
        }
    }

    private void revealCorrectPath() {
        resetAllCells();
        for (int i = 0; i < correctPath.size(); i++) {
            int[] pos = correctPath.get(i);
            gridBtns[pos[0]][pos[1]].setBackground(i == 0 ? COLOR_START : COLOR_PATH);
            gridBtns[pos[0]][pos[1]].setText(String.valueOf(i + 1));
        }
        Timer retry = new Timer(2200, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { showRetryDialog(); }
        });
        retry.setRepeats(false);
        retry.start();
    }

    // ---- Dialogs ----
    private void showLevelCompleteDialog() {
        Timer t = new Timer(700, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String[] opts = level < 10
                        ? new String[]{"Next Level", "Replay", "Home"}
                        : new String[]{"Replay", "Home"};
                int def = 0;
                int opt = JOptionPane.showOptionDialog(PathMemoryView.this,
                        "Level " + level + " Complete!\nScore: " + score,
                        "Level Complete!", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE, null, opts, opts[def]);
                if (level < 10) {
                    if (opt == 0)      startLevel(level + 1);
                    else if (opt == 1) startLevel(level);
                    else               listener.onBack();
                } else {
                    if (opt == 0) startLevel(level);
                    else          listener.onBack();
                }
            }
        });
        t.setRepeats(false);
        t.start();
    }

    private void showRetryDialog() {
        int opt = JOptionPane.showOptionDialog(this,
                "Wrong path! Try again?", "Try Again",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, new String[]{"Retry", "Home"}, "Retry");
        if (opt == 0) startLevel(level);
        else          listener.onBack();
    }

    // ---- Helpers ----
    private JLabel hudLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(UIConstants.FONT_BODY);
        l.setForeground(UIConstants.TEXT_PRIMARY);
        return l;
    }

    private JSeparator makeSep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 18));
        s.setForeground(new Color(60, 80, 120));
        return s;
    }
}