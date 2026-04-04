package view;

import model.Card;
import network.GameClient;
import network.GameServer;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * Multiplayer lobby screen.
 * Auto-fills server address and port.
 * Shows public rooms to join.
 * Allows creating private or public rooms.
 */
public class MultiplayerView extends JPanel {

    public interface MultiplayerListener {
        void onGameStart(String roomId, String username, GameClient client,
                         int rows, int cols, String[] boardValues,
                         Card.Category category, String myColor, String scoreboard,
                         String firstTurnPlayer);
        void onBack();
    }

    // Pre-configured server details (auto-filled)
    private static final String SERVER_HOST = "hopper.proxy.rlwy.net";
    private static final int    SERVER_PORT  = 14180;

    private final MultiplayerListener listener;
    private final String username;

    private GameClient client;
    private String roomId;
    private String myColor;
    private Card.Category selectedCategory = Card.Category.EMOJIS;
    private int selectedLevel = 1;

    // UI panels
    private CardLayout cardLayout;
    private JPanel     cardPanel;
    private JLabel     statusLabel;
    private JTextField roomField;
    private JLabel     errorLabel;
    private JPanel     publicRoomsPanel;

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
        box.setBorder(new EmptyBorder(20, 50, 20, 50));

        JLabel title = new JLabel("Multiplayer");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Play in real-time with friends or random players");
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(title);
        box.add(Box.createVerticalStrut(4));
        box.add(sub);
        box.add(Box.createVerticalStrut(16));

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(buildLobbyPanel(), "lobby");
        cardPanel.add(buildWaitingPanel(), "waiting");

        box.add(cardPanel);
        center.add(box);
        add(center, BorderLayout.CENTER);
    }

    // ---- Lobby Panel ----

    private JPanel buildLobbyPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Server info (read-only display)
        JPanel serverInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        serverInfo.setOpaque(false);
        serverInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel serverLbl = new JLabel("Server: " + SERVER_HOST + ":" + SERVER_PORT);
        serverLbl.setFont(UIConstants.FONT_SMALL);
        serverLbl.setForeground(UIConstants.SUCCESS_GREEN);
        JLabel connectedLbl = new JLabel("  Connected");
        connectedLbl.setFont(UIConstants.FONT_SMALL);
        connectedLbl.setForeground(UIConstants.SUCCESS_GREEN);
        serverInfo.add(serverLbl);
        serverInfo.add(connectedLbl);

        // Room ID field
        roomField = styledField();
        roomField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Level and category
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
            @Override public void actionPerformed(ActionEvent e) {
                selectedLevel = (Integer) levelBox.getSelectedItem();
            }
        });

        JLabel catLbl = new JLabel("Category:");
        catLbl.setForeground(UIConstants.TEXT_MUTED);
        catLbl.setFont(UIConstants.FONT_SMALL);
        JComboBox<String> catBox = new JComboBox<>(new String[]{"Emojis","Animals","Fruits","Shapes"});
        styleCombo(catBox);
        catBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                switch (catBox.getSelectedIndex()) {
                    case 0: selectedCategory = Card.Category.EMOJIS;  break;
                    case 1: selectedCategory = Card.Category.ANIMALS; break;
                    case 2: selectedCategory = Card.Category.FRUITS;  break;
                    case 3: selectedCategory = Card.Category.SHAPES;  break;
                }
            }
        });

        optRow.add(lvlLbl); optRow.add(levelBox);
        optRow.add(catLbl); optRow.add(catBox);

        // Public room toggle
        JCheckBox publicCheck = new JCheckBox("Make room public (anyone can join)");
        publicCheck.setOpaque(false);
        publicCheck.setForeground(UIConstants.TEXT_MUTED);
        publicCheck.setFont(UIConstants.FONT_SMALL);
        publicCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(UIConstants.FONT_SMALL);
        errorLabel.setForeground(UIConstants.ERROR_RED);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Action buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createBtn = makeButton("Create Room", UIConstants.ACCENT_BLUE);
        JButton joinBtn   = makeButton("Join Room",   UIConstants.ACCENT_PURPLE);
        JButton backBtn   = makeButton("Back",        new Color(60,60,80));

        createBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String room = roomField.getText().trim();
                if (room.isEmpty()) room = generateRoomId();
                roomField.setText(room);
                doConnect(room, true, publicCheck.isSelected());
            }
        });
        joinBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String room = roomField.getText().trim();
                if (room.isEmpty()) { errorLabel.setText("Enter a Room ID to join."); return; }
                doConnect(room, false, false);
            }
        });
        backBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onBack(); }
        });

        btnRow.add(createBtn); btnRow.add(joinBtn); btnRow.add(backBtn);

        // Public rooms section
        JLabel pubTitle = new JLabel("Online Public Rooms");
        pubTitle.setFont(UIConstants.FONT_HEADING);
        pubTitle.setForeground(UIConstants.TEXT_PRIMARY);
        pubTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        publicRoomsPanel = new JPanel();
        publicRoomsPanel.setOpaque(false);
        publicRoomsPanel.setLayout(new BoxLayout(publicRoomsPanel, BoxLayout.Y_AXIS));
        publicRoomsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton refreshBtn = makeButton("Refresh Rooms", new Color(40,60,40));
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { fetchPublicRooms(); }
        });

        // Assemble
        p.add(serverInfo);
        p.add(Box.createVerticalStrut(10));
        p.add(fieldRow("Room ID  (auto-generated if empty)", roomField));
        p.add(Box.createVerticalStrut(8));
        p.add(optRow);
        p.add(Box.createVerticalStrut(6));
        p.add(publicCheck);
        p.add(Box.createVerticalStrut(4));
        p.add(errorLabel);
        p.add(Box.createVerticalStrut(8));
        p.add(btnRow);
        p.add(Box.createVerticalStrut(20));
        p.add(pubTitle);
        p.add(Box.createVerticalStrut(8));
        p.add(publicRoomsPanel);
        p.add(Box.createVerticalStrut(6));
        p.add(refreshBtn);

        // Auto-fetch public rooms
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { fetchPublicRooms(); }
        });

        return p;
    }

    private void fetchPublicRooms() {
        // Connect temporarily to fetch room list
        GameClient tempClient = new GameClient(SERVER_HOST, SERVER_PORT);
        if (!tempClient.connect(new GameClient.MessageListener() {
            @Override public void onMessage(final String msg) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        if (msg.startsWith("PUBLIC_ROOMS:")) {
                            updatePublicRoomsList(msg.substring(13));
                        }
                        tempClient.disconnect();
                    }
                });
            }
            @Override public void onDisconnected() {}
        })) {
            updatePublicRoomsList("");
            return;
        }
        tempClient.listPublicRooms();
    }

    private void updatePublicRoomsList(String data) {
        publicRoomsPanel.removeAll();
        if (data.isEmpty()) {
            JLabel none = new JLabel("No public rooms available");
            none.setFont(UIConstants.FONT_SMALL);
            none.setForeground(UIConstants.TEXT_MUTED);
            publicRoomsPanel.add(none);
        } else {
            String[] roomEntries = data.split(",");
            for (String entry : roomEntries) {
                String[] parts = entry.split(":");
                if (parts.length < 4) continue;
                String rid      = parts[0];
                String players  = parts[1];
                String level    = parts[2];
                boolean started = Boolean.parseBoolean(parts[3]);

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
                row.setOpaque(false);

                JLabel info = new JLabel(rid + "  |  Players: " + players
                        + "  |  Level " + level
                        + (started ? "  [In Progress]" : "  [Waiting]"));
                info.setFont(UIConstants.FONT_SMALL);
                info.setForeground(UIConstants.TEXT_PRIMARY);

                JButton joinBtn = makeButton("Join", UIConstants.ACCENT_PURPLE);
                joinBtn.setPreferredSize(new Dimension(70, 28));
                final String finalRid = rid;
                joinBtn.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) {
                        roomField.setText(finalRid);
                        doConnect(finalRid, false, false);
                    }
                });

                row.add(info);
                row.add(joinBtn);
                publicRoomsPanel.add(row);
            }
        }
        publicRoomsPanel.revalidate();
        publicRoomsPanel.repaint();
    }

    // ---- Waiting Panel ----

    private JPanel buildWaitingPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel waiting = new JLabel("Waiting for players...");
        waiting.setFont(UIConstants.FONT_HEADING);
        waiting.setForeground(UIConstants.TEXT_PRIMARY);
        waiting.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel("Share your Room ID with friends");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startBtn = makeButton("Start Game Now", UIConstants.ACCENT_BLUE);
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (client != null) client.startGame(roomId);
            }
        });

        JButton cancelBtn = makeButton("Cancel", new Color(80,40,40));
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (client != null) client.disconnect();
                cardLayout.show(cardPanel, "lobby");
            }
        });

        p.add(Box.createVerticalStrut(20));
        p.add(waiting);
        p.add(Box.createVerticalStrut(12));
        p.add(statusLabel);
        p.add(Box.createVerticalStrut(20));
        p.add(startBtn);
        p.add(Box.createVerticalStrut(10));
        p.add(cancelBtn);
        return p;
    }

    // ---- Network ----

    private void doConnect(String room, boolean isCreating, boolean isPublic) {
        client = new GameClient(SERVER_HOST, SERVER_PORT);
        if (!client.connect(createListener())) {
            errorLabel.setText("Cannot connect to server. Try again.");
            return;
        }
        roomId = room;
        if (isCreating) {
            client.createRoom(room, username, selectedLevel, selectedCategory, isPublic);
        } else {
            client.joinRoom(room, username);
        }
        statusLabel.setText("Room: " + room + "  |  Waiting...");
        cardLayout.show(cardPanel, "waiting");
    }

    private GameClient.MessageListener createListener() {
        return new GameClient.MessageListener() {
            @Override public void onMessage(final String msg) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { handleServerMessage(msg); }
                });
            }
            @Override public void onDisconnected() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        errorLabel.setText("Disconnected from server.");
                        cardLayout.show(cardPanel, "lobby");
                    }
                });
            }
        };
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split(":");
        switch (parts[0]) {
            case "CREATED":
                myColor = parts[2];
                statusLabel.setText("Room: " + parts[1] + "  |  Waiting for players... (Start when ready)");
                break;
            case "JOINED":
                myColor = parts[2];
                statusLabel.setText("Joined room " + parts[1] + "! Waiting for host to start...");
                break;
            case "PLAYER_JOINED":
                statusLabel.setText(parts[1] + " joined! " + extractPlayerCount(message) + " players in room.");
                break;
            case "START": {
                // Format: START:<rows>:<cols>:<values>:<scoreboard>:<firstTurn>
                int rows = Integer.parseInt(parts[1]);
                int cols = Integer.parseInt(parts[2]);
                String[] values = parts[3].split(",");
                // Extract scoreboard and firstTurn from remaining message
                String afterValues = message.substring(
                    "START:".length() + parts[1].length() + 1
                    + parts[2].length() + 1 + parts[3].length() + 1);
                int lastColon = afterValues.lastIndexOf(":");
                String sb = lastColon > 0 ? afterValues.substring(0, lastColon) : afterValues;
                String firstTurn = lastColon > 0 ? afterValues.substring(lastColon + 1) : "";
                listener.onGameStart(roomId, username, client,
                        rows, cols, values, selectedCategory,
                        myColor != null ? myColor : "#3498DB", sb, firstTurn);
                break;
            }
            case "ERROR":
                errorLabel.setText(message.substring(6));
                cardLayout.show(cardPanel, "lobby");
                break;
        }
    }

    private String extractPlayerCount(String message) {
        // Count commas in scoreboard portion
        int count = 1;
        for (char c : message.toCharArray()) if (c == ',') count++;
        return count + "";
    }

    // ---- Helpers ----

    private String generateRoomId() { return "ROOM" + (1000 + new Random().nextInt(8999)); }

    private JPanel fieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 3));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(500, 62));
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
        f.setBackground(new Color(35,45,80));
        f.setForeground(UIConstants.TEXT_PRIMARY);
        f.setFont(UIConstants.FONT_BODY);
        f.setOpaque(true);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(64,80,130), 1),
                new EmptyBorder(6,10,6,10)));
        f.setPreferredSize(new Dimension(500, 38));
        f.setMaximumSize(new Dimension(500, 38));
        return f;
    }

    private void styleCombo(JComboBox<?> box) {
        box.setBackground(new Color(35,45,80));
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
        btn.setBorder(BorderFactory.createEmptyBorder(8,14,8,14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}