package view;

import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spot the Difference Brain Training Game.
 * Two programmatically generated images shown side by side.
 * Player must click all differences within the time limit.
 * No external image files needed - all drawn with Java2D.
 */
public class SpotDifferenceView extends JPanel {

    public interface SpotListener { void onBack(); }

    private SpotListener listener;
    private int level;
    private int score = 0;
    private int foundCount = 0;
    private int totalDiffs;
    private int timeLeft;
    private Timer gameTimer;

    // Difference data: {x, y, radius} in the RIGHT image
    private List<int[]> differences = new ArrayList<int[]>();
    private List<int[]> foundDiffs  = new ArrayList<int[]>();

    // Canvas data
    private int canvasW = 340, canvasH = 280;
    private ImageCanvas leftCanvas, rightCanvas;
    private long seed; // same seed = same shapes on both sides

    private JLabel statusLabel, levelLabel, scoreLabel, timerLabel, foundLabel;

    public SpotDifferenceView(int startLevel, SpotListener listener) {
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
        hud.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        levelLabel = hudLabel("Level 1");
        scoreLabel = hudLabel("Score: 0");
        foundLabel = hudLabel("Found: 0/3");
        left.add(levelLabel); left.add(makeSep());
        left.add(scoreLabel); left.add(makeSep());
        left.add(foundLabel);

        JLabel title = new JLabel("Spot the Difference", SwingConstants.CENTER);
        title.setFont(UIConstants.FONT_HEADING);
        title.setForeground(UIConstants.ACCENT_CYAN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        timerLabel = new JLabel("60s");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        timerLabel.setForeground(UIConstants.SUCCESS_GREEN);
        JButton homeBtn = new JButton("Home");
        homeBtn.setFont(UIConstants.FONT_SMALL);
        homeBtn.setBackground(new Color(60,60,80));
        homeBtn.setForeground(UIConstants.TEXT_PRIMARY);
        homeBtn.setOpaque(true);
        homeBtn.setFocusPainted(false);
        homeBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (gameTimer != null) gameTimer.stop();
                listener.onBack();
            }
        });
        right.add(timerLabel); right.add(homeBtn);

        hud.add(left, BorderLayout.WEST);
        hud.add(title, BorderLayout.CENTER);
        hud.add(right, BorderLayout.EAST);
        add(hud, BorderLayout.NORTH);

        statusLabel = new JLabel("Click on the differences in the RIGHT image!",
                SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(UIConstants.TEXT_MUTED);
        statusLabel.setBorder(new EmptyBorder(6, 0, 6, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void startLevel(int lvl) {
        this.level     = lvl;
        this.totalDiffs = Math.min(3 + lvl, 10);
        this.timeLeft  = Math.max(20, 60 - (lvl - 1) * 4);
        this.foundCount = 0;
        this.foundDiffs.clear();
        this.differences.clear();
        this.seed = System.nanoTime();

        levelLabel.setText("Level " + lvl);
        timerLabel.setText(timeLeft + "s");
        timerLabel.setForeground(UIConstants.SUCCESS_GREEN);
        foundLabel.setText("Found: 0/" + totalDiffs);

        generateDifferences();
        buildCanvases();
        startTimer();
    }

    private void generateDifferences() {
        Random rnd = new Random(seed + 9999);
        for (int i = 0; i < totalDiffs; i++) {
            int x = 30 + rnd.nextInt(canvasW - 60);
            int y = 30 + rnd.nextInt(canvasH - 60);
            int r = 15 + rnd.nextInt(10);
            differences.add(new int[]{x, y, r});
        }
    }

    private void buildCanvases() {
        if (leftCanvas != null)  remove(leftCanvas.getParent());

        leftCanvas  = new ImageCanvas(seed, differences, false, this::onRightClick);
        rightCanvas = new ImageCanvas(seed, differences, true,  this::onRightClick);

        JPanel canvasPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        canvasPanel.setBackground(UIConstants.BG_DARK);

        JPanel leftBox  = wrapCanvas(leftCanvas,  "Original");
        JPanel rightBox = wrapCanvas(rightCanvas, "Find Differences Here");

        canvasPanel.add(leftBox);
        canvasPanel.add(rightBox);
        add(canvasPanel, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private JPanel wrapCanvas(ImageCanvas canvas, String label) {
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setOpaque(false);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        box.add(lbl, BorderLayout.NORTH);
        box.add(canvas, BorderLayout.CENTER);
        return box;
    }

    private void onRightClick(int x, int y) {
        // Check if clicked near a difference
        for (int i = 0; i < differences.size(); i++) {
            int[] diff = differences.get(i);
            boolean alreadyFound = false;
            for (int[] f : foundDiffs) if (f == diff) { alreadyFound = true; break; }
            if (alreadyFound) continue;

            double dist = Math.sqrt(Math.pow(x - diff[0], 2) + Math.pow(y - diff[1], 2));
            if (dist <= diff[2] + 12) {
                // Found!
                foundDiffs.add(diff);
                foundCount++;
                score += 50 + (timeLeft * 2);
                scoreLabel.setText("Score: " + score);
                foundLabel.setText("Found: " + foundCount + "/" + totalDiffs);
                statusLabel.setForeground(UIConstants.SUCCESS_GREEN);
                statusLabel.setText("Found one! " + (totalDiffs - foundCount) + " more to go!");
                rightCanvas.addFoundDiff(diff);
                leftCanvas.addFoundDiff(diff);
                rightCanvas.repaint();
                leftCanvas.repaint();

                if (foundCount == totalDiffs) {
                    gameTimer.stop();
                    statusLabel.setText("All differences found!");
                    showLevelComplete();
                }
                return;
            }
        }
        // Wrong click - flash red
        statusLabel.setForeground(UIConstants.ERROR_RED);
        statusLabel.setText("Not a difference! Keep looking...");
    }

    private void startTimer() {
        if (gameTimer != null) gameTimer.stop();
        gameTimer = new Timer(1000, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                timeLeft--;
                timerLabel.setText(timeLeft + "s");
                if (timeLeft <= 10) timerLabel.setForeground(UIConstants.ERROR_RED);
                else if (timeLeft <= 20) timerLabel.setForeground(new Color(241,196,15));
                if (timeLeft <= 0) {
                    gameTimer.stop();
                    statusLabel.setForeground(UIConstants.ERROR_RED);
                    statusLabel.setText("Time up! Found " + foundCount + "/" + totalDiffs);
                    showTimeUpDialog();
                }
            }
        });
        gameTimer.start();
    }

    private void showLevelComplete() {
        Timer t = new Timer(600, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int opt = JOptionPane.showOptionDialog(SpotDifferenceView.this,
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

    private void showTimeUpDialog() {
        int opt = JOptionPane.showOptionDialog(this,
                "Time's up! You found " + foundCount + "/" + totalDiffs + " differences.",
                "Time Up!", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null,
                new String[]{"Try Again", "Home"}, "Try Again");
        if (opt == 0) startLevel(level);
        else          listener.onBack();
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

    // ---- Inner canvas class ----
    interface ClickHandler { void onClick(int x, int y); }

    class ImageCanvas extends JPanel {
        private long seed;
        private List<int[]> diffs;
        private boolean isRight;
        private ClickHandler clickHandler;
        private List<int[]> foundDiffs = new ArrayList<int[]>();

        ImageCanvas(long seed, List<int[]> diffs, boolean isRight, ClickHandler handler) {
            this.seed = seed; this.diffs = diffs;
            this.isRight = isRight; this.clickHandler = handler;
            setPreferredSize(new Dimension(canvasW, canvasH));
            setBackground(new Color(240,245,255));
            setBorder(BorderFactory.createLineBorder(
                    isRight ? UIConstants.ACCENT_BLUE : new Color(100,120,160), 2));
            if (isRight) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        clickHandler.onClick(e.getX(), e.getY());
                    }
                });
            }
        }

        void addFoundDiff(int[] diff) { foundDiffs.add(diff); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background scene using seed for consistent shapes
            Random rnd = new Random(seed);
            int numShapes = 12 + level;

            // Sky gradient
            GradientPaint sky = new GradientPaint(0,0, new Color(135,206,235),
                    0, canvasH/2, new Color(200,230,255));
            g2.setPaint(sky);
            g2.fillRect(0, 0, canvasW, canvasH/2);

            // Ground
            g2.setColor(new Color(120,180,80));
            g2.fillRect(0, canvasH/2, canvasW, canvasH/2);

            // Sun
            g2.setColor(new Color(255,220,50));
            g2.fillOval(canvasW - 70, 10, 50, 50);

            // Draw random shapes
            for (int i = 0; i < numShapes; i++) {
                int x = rnd.nextInt(canvasW - 40) + 10;
                int y = rnd.nextInt(canvasH - 40) + 10;
                int w = 20 + rnd.nextInt(40);
                int h = 15 + rnd.nextInt(30);
                float hue = rnd.nextFloat();
                Color c = Color.getHSBColor(hue, 0.7f, 0.8f);
                g2.setColor(c);
                switch (rnd.nextInt(3)) {
                    case 0: g2.fillOval(x, y, w, h); break;
                    case 1: g2.fillRect(x, y, w, h); break;
                    case 2:
                        int[] px = {x, x+w/2, x+w};
                        int[] py = {y+h, y, y+h};
                        g2.fillPolygon(px, py, 3); break;
                }
            }

            // On right image: hide difference areas (draw blank oval there)
            if (isRight) {
                for (int[] diff : diffs) {
                    boolean found = false;
                    for (int[] f : foundDiffs) if (f == diff) { found = true; break; }
                    if (!found) {
                        // Draw a subtle color change as the difference
                        g2.setColor(new Color(rnd.nextInt(200)+50,
                                rnd.nextInt(200)+50, rnd.nextInt(200)+50, 180));
                        g2.fillOval(diff[0]-diff[2], diff[1]-diff[2],
                                diff[2]*2, diff[2]*2);
                    }
                }
            }

            // Draw found circles (green rings)
            g2.setStroke(new BasicStroke(3f));
            for (int[] diff : foundDiffs) {
                g2.setColor(new Color(46,204,113,180));
                g2.drawOval(diff[0]-diff[2]-4, diff[1]-diff[2]-4,
                        (diff[2]+4)*2, (diff[2]+4)*2);
                g2.setColor(new Color(46,204,113,60));
                g2.fillOval(diff[0]-diff[2]-4, diff[1]-diff[2]-4,
                        (diff[2]+4)*2, (diff[2]+4)*2);
            }

            g2.dispose();
        }
    }
}