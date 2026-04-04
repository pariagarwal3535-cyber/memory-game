package view;

import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Main home/menu screen shown after login.
 * Now includes Quiz Mode and Brain Training options.
 */
public class HomeView extends JPanel {

    public interface HomeListener {
        void onSinglePlayer();
        void onMultiplayer();
        void onQuizMode();
        void onBrainTraining();
        void onLogout();
    }

    public HomeView(String username, HomeListener listener) {
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI(username, listener);
    }

    private void buildUI(String username, HomeListener listener) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(30, 60, 30, 60));

        // Welcome
        JLabel welcome = new JLabel("Welcome back, " + username + "!");
        welcome.setFont(UIConstants.FONT_HEADING);
        welcome.setForeground(UIConstants.TEXT_MUTED);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Memory Game");
        title.setFont(new Font("Segoe UI", Font.BOLD, 46));
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Train your brain. Test your knowledge.");
        tagline.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        tagline.setForeground(UIConstants.TEXT_MUTED);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(welcome);
        box.add(Box.createVerticalStrut(4));
        box.add(title);
        box.add(Box.createVerticalStrut(6));
        box.add(tagline);
        box.add(Box.createVerticalStrut(36));

        // Buttons
        JButton singleBtn = bigButton("Single Player",
                UIConstants.ACCENT_BLUE, UIConstants.BUTTON_HOVER);
        JButton multiBtn  = bigButton("Multiplayer",
                UIConstants.ACCENT_PURPLE, UIConstants.ACCENT_PURPLE.brighter());
        JButton quizBtn   = bigButton("Quiz Mode",
                new Color(39,174,96), new Color(46,204,113));
        JButton brainBtn  = bigButton("Brain Training",
                new Color(211,84,0), new Color(230,126,34));
        JButton logoutBtn = bigButton("Logout",
                new Color(80,40,40), new Color(120,60,60));

        singleBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onSinglePlayer(); }
        });
        multiBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onMultiplayer(); }
        });
        quizBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onQuizMode(); }
        });
        brainBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onBrainTraining(); }
        });
        logoutBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onLogout(); }
        });

        box.add(singleBtn);
        box.add(Box.createVerticalStrut(12));
        box.add(multiBtn);
        box.add(Box.createVerticalStrut(12));
        box.add(quizBtn);
        box.add(Box.createVerticalStrut(12));
        box.add(brainBtn);
        box.add(Box.createVerticalStrut(12));
        box.add(logoutBtn);

        add(box);
    }

    private JButton bigButton(String text, Color base, Color hover) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(300, 52));
        btn.setMaximumSize(new Dimension(300, 52));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setForeground(Color.WHITE);
        btn.setBackground(base);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(base);  }
        });
        return btn;
    }
}