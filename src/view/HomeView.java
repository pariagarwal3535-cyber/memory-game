package view;

import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main home/menu screen shown after login.
 * Lets the player choose Single Player, Multiplayer, or Logout.
 */
public class HomeView extends JPanel {

    public interface HomeListener {
        void onSinglePlayer();
        void onMultiplayer();
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
        box.setBorder(new EmptyBorder(40, 60, 40, 60));

        // Welcome header
        JLabel welcome = new JLabel("Welcome back, " + username + "!");
        welcome.setFont(UIConstants.FONT_HEADING);
        welcome.setForeground(UIConstants.TEXT_MUTED);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("🧠 Memory Game");
        title.setFont(new Font("Segoe UI", Font.BOLD, 42));
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Flip · Match · Win");
        tagline.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        tagline.setForeground(UIConstants.TEXT_MUTED);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Mode buttons
        StyledButton singleBtn = bigButton("🎮  Single Player",
                UIConstants.ACCENT_BLUE, UIConstants.BUTTON_HOVER);
        StyledButton multiBtn  = bigButton("🌐  Multiplayer",
                UIConstants.ACCENT_PURPLE, UIConstants.ACCENT_PURPLE.brighter());
        StyledButton logoutBtn = bigButton("🚪  Logout",
                new Color(80, 40, 40), new Color(120, 60, 60));

        singleBtn.addActionListener(e -> listener.onSinglePlayer());
        multiBtn.addActionListener(e -> listener.onMultiplayer());
        logoutBtn.addActionListener(e -> listener.onLogout());

        // Layout
        box.add(welcome);
        box.add(Box.createVerticalStrut(4));
        box.add(title);
        box.add(Box.createVerticalStrut(6));
        box.add(tagline);
        box.add(Box.createVerticalStrut(48));
        box.add(singleBtn);
        box.add(Box.createVerticalStrut(14));
        box.add(multiBtn);
        box.add(Box.createVerticalStrut(14));
        box.add(logoutBtn);

        add(box);
    }

    private StyledButton bigButton(String text, Color base, Color hover) {
        StyledButton btn = new StyledButton(text, base, hover);
        btn.setPreferredSize(new Dimension(280, 52));
        btn.setMaximumSize(new Dimension(280, 52));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        return btn;
    }
}