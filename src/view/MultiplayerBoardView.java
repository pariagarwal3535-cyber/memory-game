package view;

import model.Card;
import network.GameClient;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Game board for multiplayer sessions.
 * Receives server messages via GameClient and renders opponent moves too.
 */
public class MultiplayerBoardView extends JPanel implements GameClient.MessageListener {

    public interface MPBoardListener {
        void onHomeClicked();
    }

    // Server-provided board data
    private int rows, cols;
    private String[] values;       // flat array: row*cols+col
    private boolean[] flipped;     // face-up?
    private boolean[] matched;     // matched?

    private JButton[][] cardButtons;
    private GameClient client;
    private String roomId;
    private String myUsername;
    private Card.Category category;
    private MPBoardListener boardListener;

    // HUD
    private JLabel myScoreLabel;
    private JLabel oppScoreLabel;
    private JLabel statusLabel;

    private int myScore  = 0;
    private int oppScore = 0;
    private String opponentName = "Opponent";

    // Pending flip tracking (for MISS hide-back)
    private int pendingR1 = -1, pendingC1 = -1;
    private int pendingR2 = -1, pendingC2 = -1;

    public MultiplayerBoardView(String roomId, String myUsername, GameClient client,
                                 int rows, int cols, String[] values,
                                 Card.Category category, MPBoardListener listener) {
        this.roomId      = roomId;
        this.myUsername  = myUsername;
        this.client      = client;
        this.rows        = rows;
        this.cols        = cols;
        this.values      = values;
        this.category    = category;
        this.boardListener = listener;

        flipped = new boolean[rows * cols];
        matched = new boolean[rows * cols];

        // Set this view as the active message listener
        client.setListener(this);

        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildHUD();
        buildGrid();
    }

    // ---- HUD ----

    private void buildHUD() {
        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(UIConstants.BG_PANEL);
        hud.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Left: my score
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel youLbl = new JLabel("You");
        youLbl.setFont(UIConstants.FONT_SMALL);
        youLbl.setForeground(UIConstants.TEXT_MUTED);
        myScoreLabel = new JLabel("0");
        myScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        myScoreLabel.setForeground(UIConstants.ACCENT_CYAN);
        left.add(youLbl);
        left.add(myScoreLabel);

        // Center: status
        statusLabel = new JLabel("🎮 " + myUsername + " vs " + opponentName,
                SwingConstants.CENTER);
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_PRIMARY);

        // Right: opp score + home
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        oppScoreLabel = new JLabel("0");
        oppScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        oppScoreLabel.setForeground(UIConstants.ACCENT_PURPLE);
        JLabel oppLbl = new JLabel(opponentName);
        oppLbl.setFont(UIConstants.FONT_SMALL);
        oppLbl.setForeground(UIConstants.TEXT_MUTED);

        StyledButton homeBtn = new StyledButton("🏠",
                new Color(60,60,80), new Color(90,90,110));
        homeBtn.setPreferredSize(new Dimension(50, 32));
        homeBtn.addActionListener(e -> {
            client.quit(roomId, myUsername);
            client.disconnect();
            boardListener.onHomeClicked();
        });

        right.add(oppScoreLabel);
        right.add(oppLbl);
        right.add(homeBtn);

        hud.add(left,        BorderLayout.WEST);
        hud.add(statusLabel, BorderLayout.CENTER);
        hud.add(right,       BorderLayout.EAST);

        add(hud, BorderLayout.NORTH);
    }

    // ---- Grid ----

    private void buildGrid() {
        int maxW = UIConstants.WINDOW_WIDTH  - 60;
        int maxH = UIConstants.WINDOW_HEIGHT - 120;
        int cardW = Math.min(UIConstants.CARD_MAX_SIZE, maxW / cols);
        int cardH = Math.min(UIConstants.CARD_MAX_SIZE, maxH / rows);
        int cardSize = Math.max(UIConstants.CARD_MIN_SIZE, Math.min(cardW, cardH));

        JPanel grid = new JPanel(new GridLayout(rows, cols, 6, 6));
        grid.setBackground(UIConstants.BG_DARK);
        grid.setBorder(new EmptyBorder(16, 16, 16, 16));

        cardButtons = new JButton[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = createCardButton(cardSize);
                final int row = r, col = c;
                btn.addActionListener(e -> onCardClick(row, col));
                cardButtons[r][c] = btn;
                grid.add(btn);
            }
        }

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UIConstants.BG_DARK);
        wrapper.add(grid);
        add(wrapper, BorderLayout.CENTER);
    }

    private JButton createCardButton(int size) {
        JButton btn = new JButton("?");
        btn.setPreferredSize(new Dimension(size, size));
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, (int)(size * 0.38)));
        btn.setBackground(UIConstants.CARD_BACK);
        btn.setForeground(new Color(60, 90, 150));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(50, 80, 140), 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void onCardClick(int row, int col) {
        int idx = row * cols + col;
        if (flipped[idx] || matched[idx]) return;
        client.sendFlip(roomId, myUsername, row, col);
    }

    // ---- Server Messages ----

    @Override
    public void onMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                processMessage(message);
            }
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(MultiplayerBoardView.this,
                        "Disconnected from server.",
                        "Connection Lost", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void processMessage(String message) {
        String[] p = message.split(":");
        switch (p[0]) {
            case "FLIP_ACK": {
                // FLIP_ACK:<username>:<row>:<col>:<value>
                int r = Integer.parseInt(p[2]);
                int c = Integer.parseInt(p[3]);
                String val = p[4];
                int idx = r * cols + c;
                flipped[idx] = true;
                renderCard(r, c, val, false);
                break;
            }
            case "MATCH": {
                // MATCH:<username>:<r1>:<c1>:<r2>:<c2>:<score>
                int r1 = Integer.parseInt(p[2]), c1 = Integer.parseInt(p[3]);
                int r2 = Integer.parseInt(p[4]), c2 = Integer.parseInt(p[5]);
                int score = Integer.parseInt(p[6]);
                matched[r1*cols+c1] = true;
                matched[r2*cols+c2] = true;
                renderMatched(r1, c1);
                renderMatched(r2, c2);
                updateScore(p[1], score);
                statusLabel.setText(p[1] + " found a match! 🎉");
                break;
            }
            case "MISS": {
                // MISS:<username>:<r1>:<c1>:<r2>:<c2>
                int r1 = Integer.parseInt(p[2]), c1 = Integer.parseInt(p[3]);
                int r2 = Integer.parseInt(p[4]), c2 = Integer.parseInt(p[5]);
                statusLabel.setText(p[1] + " missed...");
                // Hide after delay
                Timer t = new Timer(900, e -> {
                    flipped[r1*cols+c1] = false;
                    flipped[r2*cols+c2] = false;
                    renderCardBack(r1, c1);
                    renderCardBack(r2, c2);
                });
                t.setRepeats(false);
                t.start();
                break;
            }
            case "GAME_OVER": {
                // GAME_OVER:<winner>:<p1>:<s1>:<p2>:<s2>
                String winner = p[1];
                String msg = winner.equals("TIE")
                    ? "It's a Tie! 🤝"
                    : winner + " wins! 🏆";
                JOptionPane.showMessageDialog(this, msg + "\n\n"
                        + p[2] + ": " + p[3] + " pts\n"
                        + p[4] + ": " + p[5] + " pts",
                        "Game Over", JOptionPane.INFORMATION_MESSAGE);
                client.disconnect();
                boardListener.onHomeClicked();
                break;
            }
        }
    }

    private void updateScore(String scorer, int score) {
        if (scorer.equals(myUsername)) {
            myScore = score;
            myScoreLabel.setText("" + myScore);
        } else {
            opponentName = scorer;
            oppScore = score;
            oppScoreLabel.setText("" + oppScore);
        }
    }

    // ---- Card Rendering ----

    private void renderCard(int r, int c, String value, boolean isMatched) {
        JButton btn = cardButtons[r][c];
        btn.setText(value);
        btn.setBackground(isMatched ? UIConstants.CARD_MATCHED : UIConstants.CARD_FACE);
        btn.setForeground(new Color(30, 30, 30));
        btn.setBorder(BorderFactory.createLineBorder(
                isMatched ? UIConstants.CARD_MATCHED : UIConstants.ACCENT_BLUE, 2));
    }

    private void renderMatched(int r, int c) {
        int idx = r * cols + c;
        renderCard(r, c, values[idx], true);
    }

    private void renderCardBack(int r, int c) {
        JButton btn = cardButtons[r][c];
        btn.setText("?");
        btn.setBackground(UIConstants.CARD_BACK);
        btn.setForeground(new Color(60, 90, 150));
        btn.setBorder(BorderFactory.createLineBorder(new Color(50, 80, 140), 1));
    }
}