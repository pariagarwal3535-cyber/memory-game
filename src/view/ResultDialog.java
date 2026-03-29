package view;

import model.GameState;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

/**
 * Dialog shown when a level is completed.
 * Options: Next Level, Replay, Go to Home.
 * Fixed size to ensure all buttons are always visible.
 */
public class ResultDialog extends JDialog {

    public enum Choice { NEXT_LEVEL, REPLAY, HOME }

    public interface ResultListener {
        void onChoice(Choice choice);
    }

    public ResultDialog(JFrame parent, GameState state, boolean hasNextLevel,
                        ResultListener listener) {
        super(parent, "Level Complete!", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        buildUI(state, hasNextLevel, listener);
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI(GameState state, boolean hasNextLevel, ResultListener listener) {

        // Root panel with custom rounded painting
        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Background
                g2.setColor(UIConstants.BG_PANEL);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                // Cyan border
                g2.setColor(UIConstants.ACCENT_CYAN);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(28, 44, 28, 44));

        // --- Trophy icon ---
        JLabel trophy = new JLabel("🏆", SwingConstants.CENTER);
        trophy.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        trophy.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Title ---
        JLabel title = new JLabel("Level " + state.getLevel() + " Complete!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Stats panel ---
        JPanel stats = new JPanel(new GridLayout(3, 2, 16, 10));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);
        stats.setMaximumSize(new Dimension(280, 90));

        addStat(stats, "Score",    String.valueOf(state.getScore()));
        addStat(stats, "Time",     state.getFrozenElapsedSeconds() + "s");
        addStat(stats, "Attempts", String.valueOf(state.getAttempts()));

        // --- Divider ---
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(new Color(60, 80, 120));
        sep.setMaximumSize(new Dimension(300, 2));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Buttons ---
        // Each button on its own row so nothing gets cut off
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (hasNextLevel) {
            JButton nextBtn = makeButton("Next Level",
                    UIConstants.ACCENT_BLUE, UIConstants.BUTTON_HOVER);
            nextBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    listener.onChoice(Choice.NEXT_LEVEL);
                }
            });
            btnPanel.add(nextBtn);
            btnPanel.add(Box.createVerticalStrut(10));
        }

        JButton replayBtn = makeButton("Replay Level",
                UIConstants.ACCENT_PURPLE, UIConstants.ACCENT_PURPLE.brighter());
        replayBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                listener.onChoice(Choice.REPLAY);
            }
        });

        JButton homeBtn = makeButton("Go to Home",
                new Color(60, 60, 90), new Color(90, 90, 120));
        homeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                listener.onChoice(Choice.HOME);
            }
        });

        btnPanel.add(replayBtn);
        btnPanel.add(Box.createVerticalStrut(10));
        btnPanel.add(homeBtn);

        // --- Assemble ---
        root.add(trophy);
        root.add(Box.createVerticalStrut(6));
        root.add(title);
        root.add(Box.createVerticalStrut(18));
        root.add(stats);
        root.add(Box.createVerticalStrut(16));
        root.add(sep);
        root.add(Box.createVerticalStrut(16));
        root.add(btnPanel);

        setContentPane(root);
    }

    private void addStat(JPanel p, String label, String value) {
        JLabel lbl = new JLabel(label, SwingConstants.RIGHT);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(UIConstants.TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 16));
        val.setForeground(UIConstants.ACCENT_CYAN);

        p.add(lbl);
        p.add(val);
    }

    private JButton makeButton(String text, Color bg, Color hover) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true; repaint();
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false; repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? hover : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setPreferredSize(new Dimension(240, 44));
        btn.setMaximumSize(new Dimension(240, 44));
        return btn;
    }
}