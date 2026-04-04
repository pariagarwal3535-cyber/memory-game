package view;

import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Brain Training menu with 3 modes:
 * Path Memory, Spot the Difference, Jigsaw Reassembly
 */
public class BrainTrainingMenuView extends JPanel {

    public interface BrainMenuListener {
        void onPathMemory();
        void onSpotDifference();
        void onJigsaw();
        void onBack();
    }

    public BrainTrainingMenuView(String username, BrainMenuListener listener) {
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI(username, listener);
    }

    private void buildUI(String username, BrainMenuListener listener) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(30, 60, 30, 60));

        JLabel title = new JLabel("Brain Training");
        title.setFont(new Font("Segoe UI", Font.BOLD, 40));
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Challenge your visual memory and spatial skills");
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(title);
        box.add(Box.createVerticalStrut(6));
        box.add(sub);
        box.add(Box.createVerticalStrut(36));

        // Mode cards
        box.add(modeCard(
            "\uD83D\uDDFA",
            "Path Memory",
            "A path is shown briefly on a grid.\nMemorize it and recreate it!",
            "Levels 1-10 | Grid grows each level",
            new Color(52,152,219),
            new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    listener.onPathMemory();
                }
            }
        ));
        box.add(Box.createVerticalStrut(14));

        box.add(modeCard(
            "\uD83D\uDD0D",
            "Spot the Difference",
            "Two similar images side by side.\nFind all the differences before time runs out!",
            "Levels 1-10 | More differences each level",
            new Color(155,89,182),
            new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    listener.onSpotDifference();
                }
            }
        ));
        box.add(Box.createVerticalStrut(14));

        box.add(modeCard(
            "\uD83E\uDDE9",
            "Jigsaw Reassembly",
            "See a complete image briefly.\nThen reassemble the shuffled pieces!",
            "Levels 1-10 | More pieces each level",
            new Color(231,76,60),
            new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    listener.onJigsaw();
                }
            }
        ));
        box.add(Box.createVerticalStrut(28));

        JButton backBtn = new JButton("Back to Home");
        backBtn.setFont(UIConstants.FONT_BUTTON);
        backBtn.setForeground(Color.WHITE);
        backBtn.setBackground(new Color(60,60,80));
        backBtn.setOpaque(true);
        backBtn.setFocusPainted(false);
        backBtn.setBorder(BorderFactory.createEmptyBorder(10,24,10,24));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onBack(); }
        });
        box.add(backBtn);

        add(box);
    }

    private JPanel modeCard(String emoji, String name, String desc,
                             String info, Color accentColor, ActionListener action) {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(new Color(25,35,65));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 2),
                new EmptyBorder(16, 20, 16, 20)));
        card.setMaximumSize(new Dimension(620, 110));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Left: emoji
        JLabel emojiLbl = new JLabel(emoji, SwingConstants.CENTER);
        emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        emojiLbl.setPreferredSize(new Dimension(60, 60));

        // Center: text
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        nameLbl.setForeground(accentColor);

        JLabel descLbl = new JLabel("<html>" + desc.replace("\n","<br>") + "</html>");
        descLbl.setFont(UIConstants.FONT_SMALL);
        descLbl.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel infoLbl = new JLabel(info);
        infoLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        infoLbl.setForeground(UIConstants.TEXT_MUTED);

        textPanel.add(nameLbl);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(descLbl);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(infoLbl);

        // Right: play button
        JButton playBtn = new JButton("Play");
        playBtn.setFont(UIConstants.FONT_BUTTON);
        playBtn.setForeground(Color.WHITE);
        playBtn.setBackground(accentColor);
        playBtn.setOpaque(true);
        playBtn.setFocusPainted(false);
        playBtn.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        playBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playBtn.addActionListener(action);

        card.add(emojiLbl, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        card.add(playBtn,   BorderLayout.EAST);

        // Make whole card clickable too
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.actionPerformed(null); }
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(new Color(35,50,85)); }
            @Override public void mouseExited (MouseEvent e) { card.setBackground(new Color(25,35,65)); }
        });

        return card;
    }
}
