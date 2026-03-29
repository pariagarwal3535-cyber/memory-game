package view;

import controller.AuthController;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Login and Registration screen.
 * Switches between Login and Register tabs.
 */
public class LoginView extends JPanel {

    public interface LoginListener {
        void onLoginSuccess(String username);
    }

    private AuthController authController;
    private LoginListener loginListener;

    // Login fields
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JLabel loginErrorLabel;

    // Register fields
    private JTextField regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmField;
    private JLabel regErrorLabel;

    // Tab state
    private boolean showingLogin = true;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    public LoginView(AuthController authController, LoginListener loginListener) {
        this.authController = authController;
        this.loginListener  = loginListener;
        setLayout(new GridBagLayout());
        setBackground(UIConstants.BG_DARK);
        buildUI();
    }

    private void buildUI() {
        JPanel centerBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_PANEL);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        centerBox.setOpaque(false);
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setBorder(new EmptyBorder(36, 44, 36, 44));
        centerBox.setPreferredSize(new Dimension(400, 480));

        // Title
        JLabel title = new JLabel("🧠 Memory Game");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerBox.add(title);
        centerBox.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Test your memory skills");
        subtitle.setFont(UIConstants.FONT_SMALL);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerBox.add(subtitle);
        centerBox.add(Box.createVerticalStrut(24));

        // Tab buttons
        JPanel tabs = new JPanel(new GridLayout(1, 2, 6, 0));
        tabs.setOpaque(false);
        tabs.setMaximumSize(new Dimension(320, 38));
        tabs.setAlignmentX(Component.CENTER_ALIGNMENT);

        StyledButton loginTab = new StyledButton("Login");
        StyledButton registerTab = new StyledButton("Register",
                new Color(60, 60, 80), new Color(80, 80, 110));

        loginTab.addActionListener(e -> {
            showingLogin = true;
            cardLayout.show(cardPanel, "login");
        });
        registerTab.addActionListener(e -> {
            showingLogin = false;
            cardLayout.show(cardPanel, "register");
        });

        tabs.add(loginTab);
        tabs.add(registerTab);
        centerBox.add(tabs);
        centerBox.add(Box.createVerticalStrut(20));

        // Card panel
        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setMaximumSize(new Dimension(320, 260));
        cardPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(buildLoginPanel(), "login");
        cardPanel.add(buildRegisterPanel(), "register");

        centerBox.add(cardPanel);

        add(centerBox);
    }

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        loginUsernameField = styledField("Username");
        loginPasswordField = styledPasswordField("Password");

        loginErrorLabel = new JLabel(" ");
        loginErrorLabel.setFont(UIConstants.FONT_SMALL);
        loginErrorLabel.setForeground(UIConstants.ERROR_RED);
        loginErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        StyledButton loginBtn = new StyledButton("Log In");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(320, 42));
        loginBtn.addActionListener(e -> doLogin());

        // Allow Enter key
        loginPasswordField.addActionListener(e -> doLogin());
        loginUsernameField.addActionListener(e -> loginPasswordField.requestFocus());

        p.add(fieldRow("Username", loginUsernameField));
        p.add(Box.createVerticalStrut(10));
        p.add(fieldRow("Password", loginPasswordField));
        p.add(Box.createVerticalStrut(6));
        p.add(loginErrorLabel);
        p.add(Box.createVerticalStrut(14));
        p.add(loginBtn);

        return p;
    }

    private JPanel buildRegisterPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        regUsernameField = styledField("Choose username");
        regPasswordField = styledPasswordField("Choose password");
        regConfirmField  = styledPasswordField("Confirm password");

        regErrorLabel = new JLabel(" ");
        regErrorLabel.setFont(UIConstants.FONT_SMALL);
        regErrorLabel.setForeground(UIConstants.ERROR_RED);
        regErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        StyledButton regBtn = new StyledButton("Create Account",
                UIConstants.ACCENT_PURPLE, UIConstants.ACCENT_PURPLE.brighter());
        regBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        regBtn.setMaximumSize(new Dimension(320, 42));
        regBtn.addActionListener(e -> doRegister());

        p.add(fieldRow("Username", regUsernameField));
        p.add(Box.createVerticalStrut(8));
        p.add(fieldRow("Password", regPasswordField));
        p.add(Box.createVerticalStrut(8));
        p.add(fieldRow("Confirm", regConfirmField));
        p.add(Box.createVerticalStrut(6));
        p.add(regErrorLabel);
        p.add(Box.createVerticalStrut(10));
        p.add(regBtn);

        return p;
    }

    private void doLogin() {
        String user = loginUsernameField.getText().trim();
        String pass = new String(loginPasswordField.getPassword());
        if (authController.login(user, pass)) {
            loginErrorLabel.setText(" ");
            if (loginListener != null) loginListener.onLoginSuccess(user);
        } else {
            loginErrorLabel.setText("Invalid username or password.");
            loginPasswordField.setText("");
        }
    }

    private void doRegister() {
        String user    = regUsernameField.getText().trim();
        String pass    = new String(regPasswordField.getPassword());
        String confirm = new String(regConfirmField.getPassword());

        String msg = authController.validateUsername(user);
        if (msg != null) { regErrorLabel.setText(msg); return; }
        msg = authController.validatePassword(pass);
        if (msg != null) { regErrorLabel.setText(msg); return; }

        if (authController.register(user, pass, confirm)) {
            regErrorLabel.setForeground(UIConstants.SUCCESS_GREEN);
            regErrorLabel.setText("Account created! Please log in.");
            regUsernameField.setText("");
            regPasswordField.setText("");
            regConfirmField.setText("");
        } else {
            regErrorLabel.setForeground(UIConstants.ERROR_RED);
            if (!pass.equals(confirm)) regErrorLabel.setText("Passwords do not match.");
            else regErrorLabel.setText("Username already taken.");
        }
    }

    // ---- Helpers ----

    private JPanel fieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(320, 58));
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        row.add(lbl, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField();
        styleInputComponent(f);
        return f;
    }

    private JPasswordField styledPasswordField(String placeholder) {
        JPasswordField f = new JPasswordField();
        styleInputComponent(f);
        return f;
    }

    private void styleInputComponent(JComponent c) {
        c.setBackground(new Color(35, 45, 80));
        c.setForeground(UIConstants.TEXT_PRIMARY);
        c.setFont(UIConstants.FONT_BODY);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(64, 80, 130), 1),
                new EmptyBorder(6, 10, 6, 10)));
        c.setPreferredSize(new Dimension(320, 38));
    }
}
