package view;

import model.Card;
import network.GameClient;
import network.GameServer;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

/**
 * Multiplayer screen - supports custom host and port for Ngrok.
 */
public class MultiplayerView extends JPanel {

    public interface MultiplayerListener {
        void onGameStart(String roomId, String username, GameClient client,
                         int rows, int cols, String[] boardValues,
                         Card.Category category);
        void onBack();
    }

    private MultiplayerListener listener;
    private String username;

    private GameClient client;
    private String roomId;
    private Card.Category selectedCategory = Card.Category.EMOJIS;
    private int selectedLevel = 1;

    private CardLayout cardLayout;
    private JPanel     cardPanel;
    private JLabel     statusLabel;
    private JTextField serverField;
    private JTextField portField;
    private JTextField roomField;
    private JLabel     errorLabel;

    public MultiplayerView(String username, MultiplayerListener listener) {
        this.username = username;
        this.listener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_DARK);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(20, 60, 20, 60));

        JLabel title = new JLabel("Multiplayer");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Play in real-time with a friend");
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(title);
        box.add(Box.createVerticalStrut(4));
        box.add(sub);
        box.add(Box.createVerticalStrut(20));

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(buildConnectionPanel(), "connect");
        cardPanel.add(buildWaitingPanel(),    "waiting");

        box.add(cardPanel);
        center.add(box);
        add(center, BorderLayout.CENTER);
    }

    private JPanel buildConnectionPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Server Address
        serverField = styledField();
        serverField.setText("localhost");

        // Port field - NEW for Ngrok support
        portField = styledField();
        portField.setText("55555");

        // Room ID
        roomField = styledField();

        // Level and category row
        JPanel optRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        optRow.setOpaque(false);
        optRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lvlLbl = new JLabel("Level:");
        lvlLbl.setForeground(UIConstants.TEXT_MUTED);
        lvlLbl.setFont(UIConstants.FONT_SMALL);

        JComboBox<Integer> levelBox = new JComboBox<Integer>();
        for (int i = 1; i <= 10; i++) levelBox.addItem(i);
        styleCombo(levelBox);
        levelBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedLevel = (Integer) levelBox.getSelectedItem();
            }
        });

        JLabel catLbl = new JLabel("Category:");
        catLbl.setForeground(UIConstants.TEXT_MUTED);
        catLbl.setFont(UIConstants.FONT_SMALL);

        JComboBox<String> catBox = new JComboBox<String>(
                new String[]{"Emojis", "Animals", "Fruits", "Shapes"});
        styleCombo(catBox);
        catBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (catBox.getSelectedIndex()) {
                    case 0: selectedCategory = Card.Category.EMOJIS;  break;
                    case 1: selectedCategory = Card.Category.ANIMALS; break;
                    case 2: selectedCategory = Card.Category.FRUITS;  break;
                    case 3: selectedCategory = Card.Category.SHAPES;  break;
                }
            }
        });

        optRow.add(lvlLbl);
        optRow.add(levelBox);
        optRow.add(catLbl);
        optRow.add(catBox);

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UIConstants.FONT_SMALL);
        errorLabel.setForeground(UIConstants.ERROR_RED);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createBtn = makeButton("Create Room", UIConstants.ACCENT_BLUE);
        JButton joinBtn   = makeButton("Join Room",   UIConstants.ACCENT_PURPLE);
        JButton backBtn   = makeButton("Back",        new Color(60, 60, 80));

        createBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String server = serverField.getText().trim();
                int    port   = parsePort();
                String room   = roomField.getText().trim();
                if (room.isEmpty()) {
                    room = generateRoomId();
                    roomField.setText(room);
                }
                doCreate(server, port, room);
            }
        });

        joinBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String server = serverField.getText().trim();
                int    port   = parsePort();
                String room   = roomField.getText().trim();
                if (room.isEmpty()) {
                    errorLabel.setText("Please enter a Room ID to join.");
                    return;
                }
                doJoin(server, port, room);
            }
        });

        backBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.onBack();
            }
        });

        btnRow.add(createBtn);
        btnRow.add(joinBtn);
        btnRow.add(backBtn);

        // Assemble
        p.add(fieldRow("Server Address  (localhost  or  Ngrok address)", serverField));
        p.add(Box.createVerticalStrut(8));
        p.add(fieldRow("Port  (55555 normally,  Ngrok port if using Ngrok)", portField));
        p.add(Box.createVerticalStrut(8));
        p.add(fieldRow("Room ID  (auto-generated if empty)", roomField));
        p.add(Box.createVerticalStrut(8));
        p.add(optRow);
        p.add(Box.createVerticalStrut(6));
        p.add(errorLabel);
        p.add(Box.createVerticalStrut(12));
        p.add(btnRow);

        return p;
    }

    private JPanel buildWaitingPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel waiting = new JLabel("Waiting for opponent...");
        waiting.setFont(UIConstants.FONT_HEADING);
        waiting.setForeground(UIConstants.TEXT_PRIMARY);
        waiting.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel("Share your Room ID with a friend");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton cancelBtn = makeButton("Cancel", new Color(80, 40, 40));
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (client != null) client.disconnect();
                cardLayout.show(cardPanel, "connect");
            }
        });

        p.add(Box.createVerticalStrut(20));
        p.add(waiting);
        p.add(Box.createVerticalStrut(12));
        p.add(statusLabel);
        p.add(Box.createVerticalStrut(20));
        p.add(cancelBtn);

        return p;
    }

    // ---- Network Actions ----

    private void doCreate(String server, int port, String room) {
        client = new GameClient(server, port);
        if (!client.connect(createListener())) {
            errorLabel.setText("Cannot connect to " + server + ":" + port);
            return;
        }
        roomId = room;
        client.createRoom(room, username, selectedLevel, selectedCategory);
        statusLabel.setText("Room: " + room + "  |  Waiting for opponent...");
        cardLayout.show(cardPanel, "waiting");
    }

    private void doJoin(String server, int port, String room) {
        client = new GameClient(server, port);
        if (!client.connect(createListener())) {
            errorLabel.setText("Cannot connect to " + server + ":" + port);
            return;
        }
        roomId = room;
        client.joinRoom(room, username);
        statusLabel.setText("Joining room " + room + "...");
        cardLayout.show(cardPanel, "waiting");
    }

    private GameClient.MessageListener createListener() {
        return new GameClient.MessageListener() {
            @Override
            public void onMessage(final String message) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        handleServerMessage(message);
                    }
                });
            }
            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        errorLabel.setText("Disconnected from server.");
                        cardLayout.show(cardPanel, "connect");
                    }
                });
            }
        };
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split(":");
        switch (parts[0]) {
            case "CREATED":
                statusLabel.setText("Room created! ID: " + parts[1]
                        + "  |  Share with your friend");
                break;
            case "JOINED":
                statusLabel.setText("Joined room " + parts[1]
                        + "  |  Host: " + parts[2]);
                break;
            case "PLAYER_JOINED":
                statusLabel.setText(parts[1] + " joined! Starting game...");
                break;
            case "START": {
                int rows = Integer.parseInt(parts[1]);
                int cols = Integer.parseInt(parts[2]);
                String[] values = parts[3].split(",");
                listener.onGameStart(roomId, username, client,
                        rows, cols, values, selectedCategory);
                break;
            }
            case "ERROR":
                errorLabel.setText(message.substring(6));
                cardLayout.show(cardPanel, "connect");
                break;
        }
    }

    // ---- Helpers ----

    private int parsePort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            return GameServer.PORT; // fallback to 55555
        }
    }

    private String generateRoomId() {
        return "ROOM" + (1000 + new Random().nextInt(8999));
    }

    private JPanel fieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 3));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(420, 62));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        row.add(lbl, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JTextField styledField() {
        JTextField f = new JTextField();
        f.setBackground(new Color(35, 45, 80));
        f.setForeground(UIConstants.TEXT_PRIMARY);
        f.setFont(UIConstants.FONT_BODY);
        f.setOpaque(true);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(64, 80, 130), 1),
                new EmptyBorder(6, 10, 6, 10)));
        f.setPreferredSize(new Dimension(420, 38));
        f.setMaximumSize(new Dimension(420, 38));
        return f;
    }

    private void styleCombo(JComboBox<?> box) {
        box.setBackground(new Color(35, 45, 80));
        box.setForeground(UIConstants.TEXT_PRIMARY);
        box.setFont(UIConstants.FONT_SMALL);
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}