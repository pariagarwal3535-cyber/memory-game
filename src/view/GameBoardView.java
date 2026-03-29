package view;

import model.Card;
import model.GameBoard;
import model.GameState;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The main game board screen.
 * The card grid dynamically fills the entire available window area.
 */
public class GameBoardView extends JPanel {

    public interface BoardListener {
        void onCardClicked(int row, int col);
        void onHomeClicked();
    }

    private GameBoard     board;
    private CardView[][]  cardViews;
    private BoardListener listener;

    private JLabel playerLabel;
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private JLabel timeLabel;

    public GameBoardView(GameBoard board, GameState state, BoardListener listener) {
        this.board    = board;
        this.listener = listener;

        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_DARK);

        add(buildHUD(state),  BorderLayout.NORTH);
        add(buildGrid(board), BorderLayout.CENTER);
    }

    // ---- HUD ----

    private JPanel buildHUD(GameState state) {
        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(UIConstants.BG_PANEL);
        hud.setBorder(new EmptyBorder(8, 20, 8, 20));
        hud.setPreferredSize(new Dimension(0, 50));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);
        playerLabel = hudLabel("Player: " + state.getPlayerName());
        levelLabel  = hudLabel("Level " + state.getLevel());
        left.add(playerLabel);
        left.add(makeSep());
        left.add(levelLabel);

        JLabel title = new JLabel("Memory Game", SwingConstants.CENTER);
        title.setFont(UIConstants.FONT_HEADING);
        title.setForeground(UIConstants.ACCENT_CYAN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);
        scoreLabel = hudLabel("Score: 0");
        timeLabel  = hudLabel("Time: 0s");

        JButton homeBtn = new JButton("Home");
        homeBtn.setFont(UIConstants.FONT_SMALL);
        homeBtn.setBackground(new Color(60, 60, 80));
        homeBtn.setForeground(UIConstants.TEXT_PRIMARY);
        homeBtn.setFocusPainted(false);
        homeBtn.setOpaque(true);
        homeBtn.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 120), 1));
        homeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        homeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.onHomeClicked();
            }
        });

        right.add(scoreLabel);
        right.add(makeSep());
        right.add(timeLabel);
        right.add(homeBtn);

        hud.add(left,  BorderLayout.WEST);
        hud.add(title, BorderLayout.CENTER);
        hud.add(right, BorderLayout.EAST);
        return hud;
    }

    private JLabel hudLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_BODY);
        l.setForeground(UIConstants.TEXT_PRIMARY);
        return l;
    }

    private JSeparator makeSep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 18));
        s.setForeground(new Color(70, 90, 130));
        return s;
    }

    // ---- Grid ----

    private JPanel buildGrid(GameBoard board) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Use a panel that calculates card size based on actual rendered size
        JPanel wrapper = new JPanel(new GridBagLayout()) {
            @Override
            public void doLayout() {
                super.doLayout();
                resizeCards();
            }
        };
        wrapper.setBackground(UIConstants.BG_DARK);

        JPanel grid = new JPanel(new GridLayout(rows, cols, 6, 6));
        grid.setBackground(UIConstants.BG_DARK);
        grid.setBorder(new EmptyBorder(10, 10, 10, 10));
        grid.setName("GRID");

        cardViews = new CardView[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Card card = board.getCard(r, c);
                // Start with a reasonable default size; resizeCards() will fix it
                CardView cv = new CardView(card, 80);
                final int row = r, col = c;
                cv.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        listener.onCardClicked(row, col);
                    }
                });
                cardViews[r][c] = cv;
                grid.add(cv);
            }
        }

        wrapper.add(grid);
        return wrapper;
    }

    /**
     * Recalculate card size to fill the available window space.
     * Called whenever the panel is laid out (including on resize).
     */
    private void resizeCards() {
        if (cardViews == null) return;

        int rows = board.getRows();
        int cols = board.getCols();

        // Available area = window minus HUD (50px) and padding
        int availW = getWidth()  - 40;
        int availH = getHeight() - 70;

        if (availW <= 0 || availH <= 0) return;

        // Calculate max card size that fits all rows and cols
        int cardW = (availW - cols * 6) / cols;
        int cardH = (availH - rows * 6) / rows;
        int cardSize = Math.min(cardW, cardH);
        cardSize = Math.max(40, Math.min(cardSize, 130)); // clamp 40–130px

        // Update all card preferred sizes
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (cardViews[r][c] != null) {
                    cardViews[r][c].setCardSize(cardSize);
                }
            }
        }
    }

    // ---- Public API ----

    public void refreshCard(int row, int col) {
        if (cardViews != null && cardViews[row][col] != null) {
            cardViews[row][col].update();
        }
    }

    public void refreshAllCards() {
        if (cardViews == null) return;
        for (CardView[] row : cardViews)
            for (CardView cv : row)
                if (cv != null) cv.update();
    }

    public void updateHUD(final String player, final int score,
                          final int level, final int seconds) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (playerLabel != null) playerLabel.setText("Player: " + player);
                if (scoreLabel  != null) scoreLabel.setText("Score: " + score);
                if (levelLabel  != null) levelLabel.setText("Level " + level);
                if (timeLabel   != null) timeLabel.setText("Time: " + seconds + "s");
            }
        });
    }
}