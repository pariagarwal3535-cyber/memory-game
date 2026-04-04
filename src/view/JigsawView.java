package view;

import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Jigsaw Reassembly Brain Training Game.
 * A colorful image is shown briefly, then cut into pieces.
 * Player drags pieces to reassemble the image.
 * All images generated programmatically - no files needed.
 */
public class JigsawView extends JPanel {

    public interface JigsawListener { void onBack(); }

    private JigsawListener listener;
    private int level;
    private int score = 0;
    private int cols, rows;
    private int previewMs;
    private int pieceW, pieceH;

    private BufferedImage fullImage;
    private List<JigsawPiece> pieces = new ArrayList<JigsawPiece>();
    private JigsawPiece dragging = null;
    private int dragOffX, dragOffY;

    private JLabel levelLabel, scoreLabel, statusLabel, progressLabel;
    private JPanel gamePanel;
    private int placedCount = 0;

    private static final int CANVAS_W = 500;
    private static final int CANVAS_H = 380;

    public JigsawView(int startLevel, JigsawListener listener) {
        this.listener = listener;
        this.level    = startLevel;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildHUD();
        startLevel(level);
    }

    private void buildHUD() {
        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(UIConstants.BG_PANEL);
        hud.setBorder(new EmptyBorder(10,20,10,20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        levelLabel   = hudLabel("Level 1");
        scoreLabel   = hudLabel("Score: 0");
        progressLabel = hudLabel("Placed: 0/4");
        left.add(levelLabel); left.add(makeSep());
        left.add(scoreLabel); left.add(makeSep());
        left.add(progressLabel);

        JLabel title = new JLabel("Jigsaw Reassembly", SwingConstants.CENTER);
        title.setFont(UIConstants.FONT_HEADING);
        title.setForeground(UIConstants.ACCENT_CYAN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JButton homeBtn = new JButton("Home");
        homeBtn.setFont(UIConstants.FONT_SMALL);
        homeBtn.setBackground(new Color(60,60,80));
        homeBtn.setForeground(UIConstants.TEXT_PRIMARY);
        homeBtn.setOpaque(true);
        homeBtn.setFocusPainted(false);
        homeBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onBack(); }
        });
        right.add(homeBtn);

        hud.add(left,  BorderLayout.WEST);
        hud.add(title, BorderLayout.CENTER);
        hud.add(right, BorderLayout.EAST);
        add(hud, BorderLayout.NORTH);

        statusLabel = new JLabel("Memorize the image!", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(UIConstants.ACCENT_CYAN);
        statusLabel.setBorder(new EmptyBorder(6, 0, 6, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void startLevel(int lvl) {
        this.level = lvl;
        this.placedCount = 0;
        // Grid: 2x2 at level 1, up to 5x4 at level 10
        cols = Math.min(2 + (lvl - 1) / 2, 5);
        rows = Math.min(2 + (lvl - 1) / 3, 4);
        previewMs = Math.max(1000, 3000 - (lvl - 1) * 200);
        pieceW = CANVAS_W / cols;
        pieceH = CANVAS_H / rows;

        levelLabel.setText("Level " + lvl);
        progressLabel.setText("Placed: 0/" + (rows * cols));

        generateImage();
        buildGamePanel();
        showPreview();
    }

    /** Generate a colorful abstract image using Java2D */
    private void generateImage() {
        fullImage = new BufferedImage(CANVAS_W, CANVAS_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = fullImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background gradient
        GradientPaint bg = new GradientPaint(0, 0,
                new Color(20 + level*10, 30, 80),
                CANVAS_W, CANVAS_H,
                new Color(60, 10 + level*8, 120));
        g2.setPaint(bg);
        g2.fillRect(0, 0, CANVAS_W, CANVAS_H);

        java.util.Random rnd = new java.util.Random(level * 7919L);

        // Draw colorful shapes to make a recognizable scene
        // Mountains
        g2.setColor(new Color(80, 120, 60));
        int[] mx = {0, 120, 240};
        int[] my = {CANVAS_H, CANVAS_H - 150, CANVAS_H};
        g2.fillPolygon(mx, my, 3);
        int[] mx2 = {150, 300, 450};
        int[] my2 = {CANVAS_H, CANVAS_H - 200, CANVAS_H};
        g2.fillPolygon(mx2, my2, 3);
        int[] mx3 = {280, CANVAS_W, CANVAS_W};
        int[] my3 = {CANVAS_H, CANVAS_H - 120, CANVAS_H};
        g2.fillPolygon(mx3, my3, 3);

        // Sun / Moon
        if (level % 2 == 0) {
            g2.setColor(new Color(255, 220, 50));
            g2.fillOval(50, 20, 70, 70);
        } else {
            g2.setColor(new Color(220, 230, 255));
            g2.fillOval(50, 20, 60, 60);
            g2.setColor(new Color(20 + level*10, 30, 80));
            g2.fillOval(68, 18, 60, 60);
        }

        // Stars / clouds
        for (int i = 0; i < 15 + level; i++) {
            int x = rnd.nextInt(CANVAS_W);
            int y = rnd.nextInt(CANVAS_H / 2);
            g2.setColor(new Color(255, 255, 255, 150 + rnd.nextInt(100)));
            g2.fillOval(x, y, 4 + rnd.nextInt(6), 4 + rnd.nextInt(6));
        }

        // Trees
        for (int i = 0; i < 5 + level; i++) {
            int tx = 30 + rnd.nextInt(CANVAS_W - 60);
            int ty = CANVAS_H - 80 - rnd.nextInt(40);
            g2.setColor(new Color(60, 100, 40));
            int[] tpx = {tx, tx + 20, tx + 40};
            int[] tpy = {ty, ty - 60, ty};
            g2.fillPolygon(tpx, tpy, 3);
            g2.setColor(new Color(100, 70, 30));
            g2.fillRect(tx + 15, ty, 10, 30);
        }

        // River
        g2.setColor(new Color(30, 100, 200, 150));
        g2.fillOval(100, CANVAS_H - 80, 300, 60);

        // Grid lines to show pieces
        g2.setColor(new Color(255,255,255,30));
        g2.setStroke(new BasicStroke(1f));
        for (int c = 1; c < cols; c++) g2.drawLine(c * pieceW, 0, c * pieceW, CANVAS_H);
        for (int r = 1; r < rows; r++) g2.drawLine(0, r * pieceH, CANVAS_W, r * pieceH);

        g2.dispose();
    }

    private void buildGamePanel() {
        if (gamePanel != null) remove(gamePanel);
        pieces.clear();

        // Create piece list
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                BufferedImage pieceImg = fullImage.getSubimage(
                        c * pieceW, r * pieceH, pieceW, pieceH);
                JigsawPiece piece = new JigsawPiece(pieceImg, r, c, pieceW, pieceH);
                pieces.add(piece);
            }
        }

        gamePanel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();

                // Draw target grid (outlines where pieces should go)
                int startX = 20, startY = 20;
                g2.setColor(new Color(60,80,130));
                g2.setStroke(new BasicStroke(1.5f));
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int x = startX + c * pieceW;
                        int y = startY + r * pieceH;
                        g2.drawRect(x, y, pieceW, pieceH);
                        // If placed, show the piece
                        for (JigsawPiece p : pieces) {
                            if (p.placed && p.targetRow == r && p.targetCol == c) {
                                g2.drawImage(p.image, x, y, null);
                            }
                        }
                    }
                }

                // Draw unplaced pieces
                for (JigsawPiece p : pieces) {
                    if (!p.placed && p != dragging) {
                        g2.drawImage(p.image, p.x, p.y, null);
                        g2.setColor(new Color(255,255,255,60));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRect(p.x, p.y, pieceW, pieceH);
                    }
                }

                // Draw dragging piece on top
                if (dragging != null) {
                    g2.drawImage(dragging.image, dragging.x, dragging.y, null);
                    g2.setColor(UIConstants.ACCENT_CYAN);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRect(dragging.x, dragging.y, pieceW, pieceH);
                }

                g2.dispose();
            }
        };
        gamePanel.setBackground(UIConstants.BG_DARK);
        gamePanel.setPreferredSize(new Dimension(CANVAS_W + 200, CANVAS_H + 80));

        // Shuffle pieces to scattered positions (right side of board)
        List<JigsawPiece> shuffled = new ArrayList<JigsawPiece>(pieces);
        Collections.shuffle(shuffled);
        int startX = CANVAS_W + 60;
        int startY = 30;
        int perRow = 3;
        for (int i = 0; i < shuffled.size(); i++) {
            JigsawPiece p = shuffled.get(i);
            p.x = startX + (i % perRow) * (pieceW + 10);
            p.y = startY + (i / perRow) * (pieceH + 10);
        }

        // Mouse listeners for drag and drop
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                for (int i = pieces.size()-1; i >= 0; i--) {
                    JigsawPiece p = pieces.get(i);
                    if (!p.placed && e.getX() >= p.x && e.getX() <= p.x + pieceW
                            && e.getY() >= p.y && e.getY() <= p.y + pieceH) {
                        dragging = p;
                        dragOffX = e.getX() - p.x;
                        dragOffY = e.getY() - p.y;
                        break;
                    }
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (dragging == null) return;
                // Check if dropped on target slot
                int targetX = 20 + dragging.targetCol * pieceW;
                int targetY = 20 + dragging.targetRow * pieceH;
                if (Math.abs(dragging.x - targetX) < pieceW/2
                        && Math.abs(dragging.y - targetY) < pieceH/2) {
                    // Snap to target
                    dragging.placed = true;
                    dragging.x = targetX;
                    dragging.y = targetY;
                    placedCount++;
                    score += 50 + level * 5;
                    scoreLabel.setText("Score: " + score);
                    progressLabel.setText("Placed: " + placedCount + "/" + (rows * cols));
                    statusLabel.setForeground(UIConstants.SUCCESS_GREEN);
                    statusLabel.setText("Good! " + (rows*cols - placedCount) + " pieces left.");

                    if (placedCount == rows * cols) {
                        statusLabel.setText("Puzzle complete!");
                        showLevelComplete();
                    }
                }
                dragging = null;
                gamePanel.repaint();
            }
        });
        gamePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (dragging != null) {
                    dragging.x = e.getX() - dragOffX;
                    dragging.y = e.getY() - dragOffY;
                    gamePanel.repaint();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(gamePanel);
        scroll.setBorder(null);
        scroll.setBackground(UIConstants.BG_DARK);
        add(scroll, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private void showPreview() {
        // Show full image briefly
        JWindow preview = new JWindow((JFrame) SwingUtilities.getWindowAncestor(this));
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBackground(Color.BLACK);

        JLabel imgLabel = new JLabel(new ImageIcon(fullImage));
        JLabel hint = new JLabel("Memorize this image! (" + previewMs/1000.0 + "s)",
                SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.BOLD, 16));
        hint.setForeground(UIConstants.ACCENT_CYAN);
        hint.setBorder(new EmptyBorder(8, 0, 8, 0));
        hint.setOpaque(true);
        hint.setBackground(Color.BLACK);

        previewPanel.add(imgLabel, BorderLayout.CENTER);
        previewPanel.add(hint, BorderLayout.SOUTH);
        preview.setContentPane(previewPanel);
        preview.pack();
        preview.setLocationRelativeTo(this);
        preview.setVisible(true);

        Timer hideTimer = new Timer(previewMs, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                preview.setVisible(false);
                preview.dispose();
                statusLabel.setText("Drag pieces to their correct positions!");
                statusLabel.setForeground(UIConstants.TEXT_PRIMARY);
            }
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private void showLevelComplete() {
        Timer t = new Timer(500, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int opt = JOptionPane.showOptionDialog(JigsawView.this,
                        "Level " + level + " complete! Score: " + score,
                        "Level Complete!",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE, null,
                        new String[]{"Next Level", "Replay", "Home"}, "Next Level");
                if (opt == 0 && level < 10) startLevel(level + 1);
                else if (opt == 1)          startLevel(level);
                else                        listener.onBack();
            }
        });
        t.setRepeats(false); t.start();
    }

    // ---- Inner class for puzzle piece ----
    static class JigsawPiece {
        BufferedImage image;
        int targetRow, targetCol;
        int x, y;
        boolean placed = false;
        int pieceW, pieceH;

        JigsawPiece(BufferedImage img, int row, int col, int pw, int ph) {
            this.image = img; this.targetRow = row;
            this.targetCol = col; this.pieceW = pw; this.pieceH = ph;
        }
    }

    private JLabel hudLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(UIConstants.FONT_BODY);
        l.setForeground(UIConstants.TEXT_PRIMARY); return l;
    }
    private JSeparator makeSep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1,18));
        s.setForeground(new Color(60,80,120)); return s;
    }
}