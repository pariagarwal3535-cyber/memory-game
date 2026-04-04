package view;

import model.Card;
import network.GameClient;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Multiplayer game board.
 * Features:
 * - Turn-based blocking (only current player can click)
 * - Player color display panel
 * - Cards matched by a player show that player's color
 * - Level complete voting dialog
 * - Multiple players support
 */
public class MultiplayerBoardView extends JPanel implements GameClient.MessageListener {

    public interface MPBoardListener { void onHomeClicked(); }

    private final int rows, cols;
    private final String[] values;
    private final boolean[] flipped;
    private final boolean[] matched;
    private final String[] matchedByColor; // color of player who matched each card

    private JButton[][] cardButtons;
    private GameClient client;
    private String roomId;
    private String myUsername;
    private Card.Category category;
    private MPBoardListener boardListener;

    // Turn state
    private String currentTurnPlayer = "";
    private boolean isMyTurn = false;

    // HUD
    private JLabel statusLabel;
    private JLabel turnLabel;
    private JPanel playerScorePanel;

    // Card size
    private int cardSize = 80;

    public MultiplayerBoardView(String roomId, String myUsername, GameClient client,
                                 int rows, int cols, String[] values,
                                 Card.Category category, String myColor,
                                 String scoreboard, String firstTurnPlayer,
                                 MPBoardListener listener) {
        this.roomId      = roomId;
        this.myUsername  = myUsername;
        this.client      = client;
        this.rows        = rows;
        this.cols        = cols;
        this.values      = values;
        this.category    = category;
        this.boardListener = listener;
        this.currentTurnPlayer = firstTurnPlayer;
        this.isMyTurn = myUsername.equals(firstTurnPlayer);

        flipped       = new boolean[rows * cols];
        matched       = new boolean[rows * cols];
        matchedByColor = new String[rows * cols];

        client.setListener(this);

        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildHUD(myColor, scoreboard);
        buildGrid();
        updateTurnLabel();
    }

    // ---- HUD ----

    private void buildHUD(String myColor, String scoreboard) {
        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(UIConstants.BG_PANEL);
        hud.setBorder(new EmptyBorder(8, 16, 8, 16));
        hud.setPreferredSize(new Dimension(0, 90));

        // Top row: title + home
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel title = new JLabel("Memory Game - Multiplayer", SwingConstants.CENTER);
        title.setFont(UIConstants.FONT_HEADING);
        title.setForeground(UIConstants.ACCENT_CYAN);

        JButton homeBtn = new JButton("Home");
        homeBtn.setFont(UIConstants.FONT_SMALL);
        homeBtn.setBackground(new Color(60,60,80));
        homeBtn.setForeground(UIConstants.TEXT_PRIMARY);
        homeBtn.setFocusPainted(false);
        homeBtn.setOpaque(true);
        homeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        homeBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                client.quit(roomId, myUsername);
                client.disconnect();
                boardListener.onHomeClicked();
            }
        });

        topRow.add(title, BorderLayout.CENTER);
        topRow.add(homeBtn, BorderLayout.EAST);

        // Bottom row: turn label + player scores
        JPanel botRow = new JPanel(new BorderLayout());
        botRow.setOpaque(false);

        turnLabel = new JLabel("", SwingConstants.LEFT);
        turnLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UIConstants.FONT_SMALL);
        statusLabel.setForeground(UIConstants.TEXT_MUTED);

        // Player scores panel
        playerScorePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        playerScorePanel.setOpaque(false);
        buildScorePanel(scoreboard, myColor);

        botRow.add(turnLabel,        BorderLayout.WEST);
        botRow.add(statusLabel,      BorderLayout.CENTER);
        botRow.add(playerScorePanel, BorderLayout.EAST);

        hud.add(topRow, BorderLayout.NORTH);
        hud.add(botRow, BorderLayout.SOUTH);

        add(hud, BorderLayout.NORTH);
    }

    private void buildScorePanel(String scoreboard, String myColor) {
        playerScorePanel.removeAll();
        if (scoreboard == null || scoreboard.isEmpty()) return;
        String[] entries = scoreboard.split(",");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length < 3) continue;
            String pName  = parts[0];
            String pScore = parts[1];
            String pColor = parts[2];

            // Declare as final so it can be used in anonymous inner class
            Color decoded;
            try {
                decoded = Color.decode(pColor);
            } catch (Exception ex) {
                decoded = Color.WHITE;
            }
            final Color color = decoded;

            JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            pill.setOpaque(true);
            // Use explicit alpha constructor
            pill.setBackground(new Color(color.getRed(), color.getGreen(),
                    color.getBlue(), 40));
            pill.setBorder(BorderFactory.createLineBorder(color, 2));

            // Color dot — uses final color safely
            JPanel dot = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(color);
                    g.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                }
            };
            dot.setPreferredSize(new Dimension(14, 14));
            dot.setOpaque(false);

            final String finalPName = pName;
            JLabel nameLbl = new JLabel(pName + ": " + pScore);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            nameLbl.setForeground(
                    finalPName.equals(myUsername) ? color : UIConstants.TEXT_PRIMARY);

            pill.add(dot);
            pill.add(nameLbl);
            playerScorePanel.add(pill);
        }
        playerScorePanel.revalidate();
        playerScorePanel.repaint();
    }

    private void updateTurnLabel() {
        if (isMyTurn) {
            turnLabel.setText("  YOUR TURN");
            turnLabel.setForeground(UIConstants.SUCCESS_GREEN);
        } else {
            turnLabel.setText("  " + currentTurnPlayer + "'s turn");
            turnLabel.setForeground(UIConstants.TEXT_MUTED);
        }
    }

    // ---- Grid ----

    private void buildGrid() {
        int availW = UIConstants.WINDOW_WIDTH  - 40;
        int availH = UIConstants.WINDOW_HEIGHT - 110;
        int cw = (availW - cols * 6) / cols;
        int ch = (availH - rows * 6) / rows;
        cardSize = Math.max(40, Math.min(Math.min(cw, ch), 120));

        JPanel grid = new JPanel(new GridLayout(rows, cols, 6, 6));
        grid.setBackground(UIConstants.BG_DARK);
        grid.setBorder(new EmptyBorder(10, 10, 10, 10));
        cardButtons = new JButton[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = createCardButton(cardSize, r, c);
                final int row = r, col = c;
                btn.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) {
                        onCardClick(row, col);
                    }
                });
                cardButtons[r][c] = btn;
                grid.add(btn);
            }
        }

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UIConstants.BG_DARK);
        wrapper.add(grid);
        add(wrapper, BorderLayout.CENTER);
    }

    /**
     * Custom card button that stores its own row/col index.
     * Renders different states: face-down, face-up, matched (with player color).
     */
    private class MPCardButton extends JButton {
        private final int row;
        private final int col;

        MPCardButton(int row, int col, int size) {
            super("?");
            this.row = row;
            this.col = col;
            setPreferredSize(new Dimension(size, size));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            int idx = row * cols + col;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), arc = 10;

            if (matched[idx]) {
                String colorStr = matchedByColor[idx] != null ? matchedByColor[idx] : "#2ECC71";
                Color mcDecoded;
                try { mcDecoded = Color.decode(colorStr); } catch (Exception ex) { mcDecoded = new Color(46,204,113); }
                Color mc = mcDecoded;
                g2.setColor(mc);
                g2.fill(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
                g2.setColor(mc.brighter());
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, (int)(h * 0.36)));
                FontMetrics fm = g2.getFontMetrics();
                String val = values[idx];
                g2.drawString(val, (w - fm.stringWidth(val)) / 2,
                        (h + fm.getAscent() - fm.getDescent()) / 2);

            } else if (flipped[idx]) {
                g2.setColor(UIConstants.CARD_FACE);
                g2.fill(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
                g2.setColor(UIConstants.ACCENT_BLUE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
                g2.setColor(new Color(30, 30, 30));
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, (int)(h * 0.36)));
                FontMetrics fm = g2.getFontMetrics();
                String val = values[idx];
                g2.drawString(val, (w - fm.stringWidth(val)) / 2,
                        (h + fm.getAscent() - fm.getDescent()) / 2);

            } else {
                g2.setColor(UIConstants.CARD_BACK);
                g2.fill(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
                if (!isMyTurn) {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fill(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
                }
                g2.setColor(new Color(80, 120, 200));
                g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(h * 0.36)));
                FontMetrics fm = g2.getFontMetrics();
                String qm = "?";
                g2.drawString(qm, (w - fm.stringWidth(qm)) / 2,
                        (h + fm.getAscent() - fm.getDescent()) / 2);
                g2.setColor(new Color(60, 90, 160));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
            }
            g2.dispose();
        }
    }

    private JButton createCardButton(int size, int row, int col) {
        return new MPCardButton(row, col, size);
    }

    private void onCardClick(int row, int col) {
        int idx = row * cols + col;
        if (!isMyTurn || flipped[idx] || matched[idx]) return;
        client.sendFlip(roomId, myUsername, row, col);
    }

    // ---- Server Messages ----

    @Override
    public void onMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { processMessage(message); }
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JOptionPane.showMessageDialog(MultiplayerBoardView.this,
                        "Disconnected from server.", "Connection Lost",
                        JOptionPane.WARNING_MESSAGE);
                boardListener.onHomeClicked();
            }
        });
    }

    private void processMessage(String message) {
        String[] p = message.split(":");
        switch (p[0]) {
            case "FLIP_ACK": {
                int r = Integer.parseInt(p[2]);
                int c = Integer.parseInt(p[3]);
                flipped[r * cols + c] = true;
                cardButtons[r][c].repaint();
                break;
            }
            case "MATCH": {
                // MATCH:<username>:<r1>:<c1>:<r2>:<c2>:<#color>:<scoreboard>
                // p[6] is the hex color like #E74C3C
                if (p.length < 7) break;
                int r1 = Integer.parseInt(p[2]), c1 = Integer.parseInt(p[3]);
                int r2 = Integer.parseInt(p[4]), c2 = Integer.parseInt(p[5]);
                String color = p[6];

                // Scoreboard comes after color field
                int colorEnd = "MATCH:".length() + p[1].length() + 1
                        + p[2].length() + 1 + p[3].length() + 1
                        + p[4].length() + 1 + p[5].length() + 1
                        + p[6].length() + 1;
                String sb = colorEnd < message.length() ? message.substring(colorEnd) : "";

                matched[r1 * cols + c1] = true;
                matched[r2 * cols + c2] = true;
                matchedByColor[r1 * cols + c1] = color;
                matchedByColor[r2 * cols + c2] = color;
                cardButtons[r1][c1].repaint();
                cardButtons[r2][c2].repaint();

                if (!sb.isEmpty()) buildScorePanel(sb, "");
                statusLabel.setText(p[1] + " matched!");
                break;
            }
            case "MISS": {
                int r1 = Integer.parseInt(p[2]), c1 = Integer.parseInt(p[3]);
                int r2 = Integer.parseInt(p[4]), c2 = Integer.parseInt(p[5]);
                statusLabel.setText(p[1] + " missed...");
                Timer t = new Timer(900, new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) {
                        flipped[r1*cols+c1] = false;
                        flipped[r2*cols+c2] = false;
                        cardButtons[r1][c1].repaint();
                        cardButtons[r2][c2].repaint();
                    }
                });
                t.setRepeats(false);
                t.start();
                break;
            }
            case "TURN": {
                currentTurnPlayer = p[1];
                isMyTurn = myUsername.equals(currentTurnPlayer);
                updateTurnLabel();
                repaintAllCards();
                break;
            }
            case "PLAYER_JOINED": {
                // PLAYER_JOINED:<username>:<#color>:<scoreboard>
                if (p.length >= 3) {
                    int sbStart = "PLAYER_JOINED:".length() + p[1].length() + 1
                            + p[2].length() + 1;
                    String sb = sbStart < message.length() ? message.substring(sbStart) : "";
                    if (!sb.isEmpty()) buildScorePanel(sb, "");
                }
                statusLabel.setText(p[1] + " joined the room!");
                break;
            }
            case "PLAYER_LEFT": {
                int sbStart = "PLAYER_LEFT:".length() + p[1].length() + 1;
                String sb = sbStart < message.length() ? message.substring(sbStart) : "";
                if (!sb.isEmpty()) buildScorePanel(sb, "");
                statusLabel.setText(p[1] + " left the game.");
                break;
            }
            case "LEVEL_COMPLETE": {
                // LEVEL_COMPLETE:<winner>:<scoreboard>:<level>
                String winner = p[1];
                String levelStr = p[p.length - 1];
                int level = 1;
                try { level = Integer.parseInt(levelStr); } catch (Exception ex) {}
                int sbStart = "LEVEL_COMPLETE:".length() + p[1].length() + 1;
                int sbEnd   = message.lastIndexOf(":");
                String sb   = (sbStart < sbEnd) ? message.substring(sbStart, sbEnd) : "";
                showLevelCompleteDialog(winner, sb, level);
                break;
            }
            case "NEXT_LEVEL":
                statusLabel.setText("Moving to Level " + p[1] + "!");
                break;
            case "REPLAY_LEVEL":
                statusLabel.setText("Replaying Level " + p[1] + "...");
                break;
            case "VOTE_UPDATE":
                statusLabel.setText("Votes: " + p[1] + "/" + p[2] + " submitted");
                break;
            case "ERROR":
                statusLabel.setText("Error: " + message.substring(6));
                break;
        }
    }

    private void showLevelCompleteDialog(String winner, String scoreboard, int level) {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this),
                "Level Complete!", true);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel();
        panel.setBackground(UIConstants.BG_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 36, 24, 36));

        JLabel winLbl = new JLabel(winner.equals("TIE") ? "It's a Tie!" : winner + " wins!", SwingConstants.CENTER);
        winLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        winLbl.setForeground(UIConstants.ACCENT_CYAN);
        winLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreLbl = new JLabel("Level " + level + " complete!", SwingConstants.CENTER);
        scoreLbl.setFont(UIConstants.FONT_BODY);
        scoreLbl.setForeground(UIConstants.TEXT_MUTED);
        scoreLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel voteLbl = new JLabel("Vote to continue:", SwingConstants.CENTER);
        voteLbl.setFont(UIConstants.FONT_SMALL);
        voteLbl.setForeground(UIConstants.TEXT_MUTED);
        voteLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);

        JButton nextBtn   = makeDialogBtn("Next Level", UIConstants.ACCENT_BLUE);
        JButton replayBtn = makeDialogBtn("Replay", UIConstants.ACCENT_PURPLE);
        JButton homeBtn   = makeDialogBtn("Home", new Color(80,40,40));

        nextBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                client.sendVote(roomId, myUsername, true);
                dialog.dispose();
            }
        });
        replayBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                client.sendVote(roomId, myUsername, false);
                dialog.dispose();
            }
        });
        homeBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                client.quit(roomId, myUsername);
                client.disconnect();
                boardListener.onHomeClicked();
            }
        });

        btnRow.add(nextBtn);
        btnRow.add(replayBtn);
        btnRow.add(homeBtn);

        panel.add(winLbl);
        panel.add(Box.createVerticalStrut(8));
        panel.add(scoreLbl);
        panel.add(Box.createVerticalStrut(12));
        panel.add(voteLbl);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnRow);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JButton makeDialogBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8,14,8,14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeButton(String text, Color bg) { return makeDialogBtn(text, bg); }

    private void repaintAllCards() {
        if (cardButtons == null) return;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (cardButtons[r][c] != null) cardButtons[r][c].repaint();
    }
}