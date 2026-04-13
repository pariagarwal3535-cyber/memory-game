package view;

import model.Card;
import quiz.Question;
import quiz.QuizBank;
import quiz.QuizController;
import network.GameClient;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * Multiplayer lobby - completely rewritten for reliability.
 * Auto-fills server address and port.
 * Shows public rooms list.
 * Simple create/join flow.
 */
public class MultiplayerView extends JPanel {

    public interface MultiplayerListener {
        void onGameStart(String roomId, String username, GameClient client,
                         int rows, int cols, String[] boardValues,
                         Card.Category category, String myColor,
                         String scoreboard, String firstTurnPlayer);
        void onQuizStart(String roomId, String username, GameClient client,
                         String myColor, String scoreboard, String firstTurnPlayer,
                         Question.Subject subject);
        void onBack();
    }

    // ---- Server config (auto-filled) ----
    private static final String SERVER_HOST = "crossover.proxy.rlwy.net";
    private static final int    SERVER_PORT = 31468;

    private final MultiplayerListener listener;
    private final String username;

    private GameClient client;
    private String roomId;
    private String myColor = "#3498DB";
    private Card.Category selectedCategory = Card.Category.EMOJIS;
    private int selectedLevel = 1;
    private boolean quizMode = false;  // true = quiz multiplayer, false = card game

    // UI
    private CardLayout cardLayout;
    private JPanel     cardPanel;
    private JLabel     statusLabel;
    private JTextField roomField;
    private JLabel     errorLabel;
    private JPanel     roomListPanel;

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
        box.setBorder(new EmptyBorder(16, 50, 16, 50));

        JLabel title = new JLabel("Multiplayer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Play in real-time with friends or anyone online");
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

        // Server status (auto-filled, read only)
        JLabel serverInfo = new JLabel("Server: " + SERVER_HOST + ":" + SERVER_PORT
                + "  [Auto-connected]");
        serverInfo.setFont(UIConstants.FONT_SMALL);
        serverInfo.setForeground(UIConstants.SUCCESS_GREEN);
        serverInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Room ID field
        JLabel roomLbl = new JLabel("Room ID  (leave empty to auto-generate)");
        roomLbl.setFont(UIConstants.FONT_SMALL);
        roomLbl.setForeground(UIConstants.TEXT_MUTED);
        roomLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        roomField = new JTextField();
        roomField.setBackground(new Color(35, 45, 80));
        roomField.setForeground(UIConstants.TEXT_PRIMARY);
        roomField.setFont(UIConstants.FONT_BODY);
        roomField.setOpaque(true);
        roomField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(64,80,130), 1),
                new EmptyBorder(6,10,6,10)));
        roomField.setMaximumSize(new Dimension(500, 38));
        roomField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Options row
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
        JComboBox<String> catBox = new JComboBox<String>(
                new String[]{"Emojis","Animals","Fruits","Shapes"});
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

        JCheckBox publicCheck = new JCheckBox("Make room public");
        publicCheck.setOpaque(false);
        publicCheck.setForeground(UIConstants.TEXT_MUTED);
        publicCheck.setFont(UIConstants.FONT_SMALL);

        // Game mode selection
        JLabel modeLbl = new JLabel("Mode:");
        modeLbl.setForeground(UIConstants.TEXT_MUTED);
        modeLbl.setFont(UIConstants.FONT_SMALL);
        JComboBox<String> modeBox = new JComboBox<String>(
                new String[]{"Card Game", "Quiz"});
        styleCombo(modeBox);
        modeBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                quizMode = modeBox.getSelectedIndex() == 1;
                // Disable category/level if quiz mode
                levelBox.setEnabled(!quizMode);
                catBox.setEnabled(!quizMode);
            }
        });

        optRow.add(lvlLbl); optRow.add(levelBox);
        optRow.add(catLbl); optRow.add(catBox);
        optRow.add(modeLbl); optRow.add(modeBox);
        optRow.add(publicCheck);

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UIConstants.FONT_SMALL);
        errorLabel.setForeground(UIConstants.ERROR_RED);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Action buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createBtn = makeBtn("Create Room", UIConstants.ACCENT_BLUE);
        JButton joinBtn   = makeBtn("Join Room",   UIConstants.ACCENT_PURPLE);
        JButton backBtn   = makeBtn("Back",        new Color(60,60,80));

        createBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String room = roomField.getText().trim();
                if (room.isEmpty()) {
                    room = "ROOM" + (1000 + new Random().nextInt(8999));
                    roomField.setText(room);
                }
                doCreate(room, publicCheck.isSelected());
            }
        });

        joinBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String room = roomField.getText().trim();
                if (room.isEmpty()) {
                    errorLabel.setText("Please enter a Room ID to join.");
                    return;
                }
                doJoin(room);
            }
        });

        backBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                listener.onBack();
            }
        });

        btnRow.add(createBtn);
        btnRow.add(joinBtn);
        btnRow.add(backBtn);

        // Online rooms section
        JLabel roomsTitle = new JLabel("Online Public Rooms");
        roomsTitle.setFont(UIConstants.FONT_HEADING);
        roomsTitle.setForeground(UIConstants.TEXT_PRIMARY);
        roomsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        roomListPanel = new JPanel();
        roomListPanel.setOpaque(false);
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton refreshBtn = makeBtn("Refresh Rooms", new Color(40,70,40));
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { fetchRooms(); }
        });

        // Assemble
        p.add(serverInfo);
        p.add(Box.createVerticalStrut(10));
        p.add(roomLbl);
        p.add(Box.createVerticalStrut(4));
        p.add(roomField);
        p.add(Box.createVerticalStrut(8));
        p.add(optRow);
        p.add(Box.createVerticalStrut(4));
        p.add(errorLabel);
        p.add(Box.createVerticalStrut(8));
        p.add(btnRow);
        p.add(Box.createVerticalStrut(18));
        p.add(roomsTitle);
        p.add(Box.createVerticalStrut(8));
        p.add(roomListPanel);
        p.add(Box.createVerticalStrut(6));
        p.add(refreshBtn);

        // Auto-fetch rooms on load
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { fetchRooms(); }
        });

        return p;
    }

    // ---- Waiting Panel ----
    private JPanel buildWaitingPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel heading = new JLabel("Room Ready!", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(UIConstants.ACCENT_CYAN);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel("Waiting for players...", SwingConstants.CENTER);
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel(
                "<html><center>Share your Room ID with friends.<br>"
                + "When ready, click <b>Start Game</b></center></html>",
                SwingConstants.CENTER);
        hint.setFont(UIConstants.FONT_SMALL);
        hint.setForeground(UIConstants.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startBtn = makeBtn("Start Game", UIConstants.ACCENT_BLUE);
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        startBtn.setPreferredSize(new Dimension(200, 50));
        startBtn.setMaximumSize(new Dimension(200, 50));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (client != null && roomId != null && !roomId.isEmpty()) {
                    statusLabel.setText("Starting...");
                    statusLabel.setForeground(UIConstants.SUCCESS_GREEN);
                    client.startGame(roomId);
                }
            }
        });

        JButton cancelBtn = makeBtn("Cancel", new Color(100, 40, 40));
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (client != null) {
                    client.quit(roomId, username);
                    client.disconnect();
                }
                client  = null;
                roomId  = null;
                myColor = "#3498DB";
                errorLabel.setText(" ");
                cardLayout.show(cardPanel, "lobby");
            }
        });

        p.add(heading);
        p.add(Box.createVerticalStrut(10));
        p.add(statusLabel);
        p.add(Box.createVerticalStrut(8));
        p.add(hint);
        p.add(Box.createVerticalStrut(28));
        p.add(startBtn);
        p.add(Box.createVerticalStrut(14));
        p.add(cancelBtn);
        return p;
    }

    // ---- Network Actions ----

    private void doCreate(String room, boolean isPublic) {
        errorLabel.setText("Connecting...");
        client = new GameClient(SERVER_HOST, SERVER_PORT);
        if (!client.connect(makeListener())) {
            errorLabel.setText("Cannot connect to server. Check internet connection.");
            client = null;
            return;
        }
        roomId = room;
        client.createRoom(room, username, selectedLevel, selectedCategory, isPublic);
        statusLabel.setText("Room: " + room + "  |  Waiting for players...");
        cardLayout.show(cardPanel, "waiting");
        errorLabel.setText(" ");
    }

    private void doJoin(String room) {
        errorLabel.setText("Connecting...");
        client = new GameClient(SERVER_HOST, SERVER_PORT);
        if (!client.connect(makeListener())) {
            errorLabel.setText("Cannot connect to server. Check internet connection.");
            client = null;
            return;
        }
        roomId = room;
        client.joinRoom(room, username);
        statusLabel.setText("Joining room " + room + "...");
        cardLayout.show(cardPanel, "waiting");
        errorLabel.setText(" ");
    }

    private void fetchRooms() {
        GameClient temp = new GameClient(SERVER_HOST, SERVER_PORT);
        final boolean[] done = {false};
        if (!temp.connect(new GameClient.MessageListener() {
            @Override public void onMessage(final String msg) {
                if (done[0]) return;
                done[0] = true;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        if (msg.startsWith("LIST:")) {
                            showRoomList(msg.substring(5));
                        }
                        temp.disconnect();
                    }
                });
            }
            @Override public void onDisconnected() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { showRoomList(""); }
                });
            }
        })) {
            showRoomList("");
            return;
        }
        temp.listRooms();
        // Auto-disconnect after 3s if no response
        Timer t = new Timer(3000, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!done[0]) { done[0] = true; temp.disconnect(); }
            }
        });
        t.setRepeats(false); t.start();
    }

    private void showRoomList(String data) {
        roomListPanel.removeAll();
        if (data == null || data.trim().isEmpty()) {
            JLabel none = new JLabel("No public rooms found. Create one!");
            none.setFont(UIConstants.FONT_SMALL);
            none.setForeground(UIConstants.TEXT_MUTED);
            roomListPanel.add(none);
        } else {
            // Format: room1~players~level~started|room2~...
            String[] entries = data.split("\\|");
            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                String[] parts = entry.split("~");
                if (parts.length < 4) continue;
                String rid     = parts[0];
                String players = parts[1];
                String level   = parts[2];
                boolean started = "1".equals(parts[3]);

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
                row.setOpaque(false);

                JLabel info = new JLabel(
                        rid + "   Players: " + players
                        + "   Level " + level
                        + (started ? "   [In Progress]" : "   [Waiting]"));
                info.setFont(UIConstants.FONT_SMALL);
                info.setForeground(started
                        ? UIConstants.TEXT_MUTED : UIConstants.TEXT_PRIMARY);

                JButton joinBtn = makeBtn("Join", UIConstants.ACCENT_PURPLE);
                joinBtn.setPreferredSize(new Dimension(70, 26));
                final String finalRid = rid;
                joinBtn.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) {
                        roomField.setText(finalRid);
                        doJoin(finalRid);
                    }
                });
                joinBtn.setEnabled(!started);

                row.add(info);
                row.add(joinBtn);
                roomListPanel.add(row);
            }
        }
        roomListPanel.revalidate();
        roomListPanel.repaint();
    }

    private GameClient.MessageListener makeListener() {
        return new GameClient.MessageListener() {
            @Override public void onMessage(final String msg) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { handleMessage(msg); }
                });
            }
            @Override public void onDisconnected() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        statusLabel.setText("Disconnected from server.");
                        statusLabel.setForeground(UIConstants.ERROR_RED);
                        cardLayout.show(cardPanel, "lobby");
                        errorLabel.setText("Disconnected. Try again.");
                    }
                });
            }
        };
    }

    private void handleMessage(String msg) {
        System.out.println("[Client received] " + msg.substring(0, Math.min(80, msg.length())));

        if (msg.startsWith("CREATED:")) {
            // CREATED:roomId:color
            String[] p = msg.split(":");
            if (p.length >= 3) myColor = p[2];
            statusLabel.setText("Room created! Share Room ID: " + roomId);
            statusLabel.setForeground(UIConstants.SUCCESS_GREEN);
            fetchRooms();

        } else if (msg.startsWith("JOINED:")) {
            // JOINED:roomId:color:scoreboard
            String[] p = msg.split(":", 4);
            if (p.length >= 3) myColor = p[2];
            statusLabel.setText("Joined! Waiting for host to start...");
            statusLabel.setForeground(UIConstants.TEXT_PRIMARY);

        } else if (msg.startsWith("PLAYER_JOINED:")) {
            // PLAYER_JOINED:username:color:scoreboard
            String[] p = msg.split(":", 4);
            String who = p.length >= 2 ? p[1] : "Someone";
            statusLabel.setText(who + " joined the room!");
            statusLabel.setForeground(UIConstants.SUCCESS_GREEN);

        } else if (msg.startsWith("GAMESTART:")) {
            // GAMESTART:rows:cols:values:scoreboard:firstTurn
            parseAndStartGame(msg);

        } else if (msg.startsWith("ERROR:")) {
            String err = msg.substring(6);
            statusLabel.setText("Error: " + err);
            statusLabel.setForeground(UIConstants.ERROR_RED);
            errorLabel.setText(err);
            cardLayout.show(cardPanel, "lobby");
        }
    }

    private void parseAndStartGame(String msg) {
        try {
            // GAMESTART:rows:cols:v1,v2,...:scoreboard:firstTurn
            // Split only first 4 colons
            String body = msg.substring("GAMESTART:".length());
            String[] parts = body.split(":", 4);
            // parts[0] = rows
            // parts[1] = cols
            // parts[2] = values (comma separated)
            // parts[3] = scoreboard:firstTurn

            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            String[] values = parts[2].split(",");

            // parts[3] = "scoreboard:firstTurn"
            // scoreboard uses ~ and | so last colon = separator before firstTurn
            String rest = parts[3];
            int lastColon = rest.lastIndexOf(":");
            String scoreboard  = lastColon >= 0 ? rest.substring(0, lastColon) : "";
            String firstTurn   = lastColon >= 0 ? rest.substring(lastColon + 1) : username;

            String color = (myColor != null && !myColor.isEmpty()) ? myColor : "#3498DB";

            if (quizMode) {
                listener.onQuizStart(roomId, username, client,
                        color, scoreboard, firstTurn,
                        Question.Subject.GK);
            } else {
                listener.onGameStart(roomId, username, client,
                        rows, cols, values, selectedCategory,
                        color, scoreboard, firstTurn);
            }

        } catch (Exception e) {
            System.err.println("[Client] Failed to parse GAMESTART: " + e.getMessage());
            errorLabel.setText("Failed to start game. Try again.");
            cardLayout.show(cardPanel, "lobby");
        }
    }

    // ---- Helpers ----
    private JButton makeBtn(String text, Color bg) {
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

    private void styleCombo(JComboBox<?> box) {
        box.setBackground(new Color(35,45,80));
        box.setForeground(UIConstants.TEXT_PRIMARY);
        box.setFont(UIConstants.FONT_SMALL);
    }
}