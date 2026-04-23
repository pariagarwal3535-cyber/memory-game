package view;

import model.Card;
import quiz.Question;
import network.GameClient;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * Multiplayer lobby.
 * Supports both card-game rooms and quiz rooms.
 * Auto-fills server address.
 */
public class MultiplayerView extends JPanel {

    public interface MultiplayerListener {
        void onGameStart(String roomId, String username, GameClient client,
                         int rows, int cols, String[] boardValues,
                         Card.Category category, String myColor,
                         String scoreboard, String firstTurnPlayer);
        void onQuizStart(String roomId, String username, GameClient client,
                         int totalQuestions, String myColor,
                         String scoreboard, String firstTurnPlayer,
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
    private boolean isHost = false;
    private Card.Category selectedCategory = Card.Category.EMOJIS;
    private int selectedLevel = 1;
    private boolean quizMode = false;
    private Question.Subject selectedSubject = Question.Subject.GK;
    private int selectedQuestionCount = 10;
    private int totalQuestionsFromServer = 10;

    // UI
    private CardLayout cardLayout;
    private JPanel     cardPanel;
    private JLabel     statusLabel;
    private JLabel     hostStatusLabel;
    private JButton    startBtn;
    private JTextField roomField;
    private JLabel     errorLabel;
    private JPanel     roomListPanel;

    // Mode-specific controls
    private JComboBox<Integer> levelBox;
    private JComboBox<String>  catBox;
    private JComboBox<String>  subjectBox;
    private JComboBox<Integer> qCountBox;
    private JLabel levelLbl, catLbl, subjectLbl, qCountLbl;

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

        JLabel serverInfo = new JLabel("Server: " + SERVER_HOST + ":" + SERVER_PORT
                + "  [Auto-connected]");
        serverInfo.setFont(UIConstants.FONT_SMALL);
        serverInfo.setForeground(UIConstants.SUCCESS_GREEN);
        serverInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        // Options row 1: mode
        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        modeRow.setOpaque(false);
        modeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel modeLbl = new JLabel("Mode:");
        modeLbl.setForeground(UIConstants.TEXT_MUTED);
        modeLbl.setFont(UIConstants.FONT_SMALL);
        final JComboBox<String> modeBox = new JComboBox<String>(
                new String[]{"Card Game", "Quiz"});
        styleCombo(modeBox);

        JCheckBox publicCheck = new JCheckBox("Make room public");
        publicCheck.setOpaque(false);
        publicCheck.setForeground(UIConstants.TEXT_MUTED);
        publicCheck.setFont(UIConstants.FONT_SMALL);

        modeRow.add(modeLbl); modeRow.add(modeBox);
        modeRow.add(publicCheck);

        // Options row 2: card-game specific (level + category)
        JPanel cardOptsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        cardOptsRow.setOpaque(false);
        cardOptsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        levelLbl = new JLabel("Level:");
        levelLbl.setForeground(UIConstants.TEXT_MUTED);
        levelLbl.setFont(UIConstants.FONT_SMALL);
        levelBox = new JComboBox<Integer>();
        for (int i = 1; i <= 10; i++) levelBox.addItem(i);
        styleCombo(levelBox);
        levelBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                selectedLevel = (Integer) levelBox.getSelectedItem();
            }
        });

        catLbl = new JLabel("Category:");
        catLbl.setForeground(UIConstants.TEXT_MUTED);
        catLbl.setFont(UIConstants.FONT_SMALL);
        catBox = new JComboBox<String>(
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

        cardOptsRow.add(levelLbl); cardOptsRow.add(levelBox);
        cardOptsRow.add(catLbl);   cardOptsRow.add(catBox);

        // Options row 3: quiz specific (subject + question count)
        final JPanel quizOptsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        quizOptsRow.setOpaque(false);
        quizOptsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        subjectLbl = new JLabel("Subject:");
        subjectLbl.setForeground(UIConstants.TEXT_MUTED);
        subjectLbl.setFont(UIConstants.FONT_SMALL);
        subjectBox = new JComboBox<String>(new String[]{
                "GK", "ENGLISH", "OPERATING_SYSTEMS", "DATA_STRUCTURES",
                "COMPUTER_NETWORKS", "DBMS", "OOP", "ALGORITHMS"
        });
        styleCombo(subjectBox);
        subjectBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                try {
                    selectedSubject = Question.Subject.valueOf(
                            (String) subjectBox.getSelectedItem());
                } catch (Exception ignored) {}
            }
        });

        qCountLbl = new JLabel("Questions:");
        qCountLbl.setForeground(UIConstants.TEXT_MUTED);
        qCountLbl.setFont(UIConstants.FONT_SMALL);
        qCountBox = new JComboBox<Integer>(new Integer[]{5, 10, 15, 20});
        qCountBox.setSelectedItem(10);
        styleCombo(qCountBox);
        qCountBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                selectedQuestionCount = (Integer) qCountBox.getSelectedItem();
            }
        });

        quizOptsRow.add(subjectLbl); quizOptsRow.add(subjectBox);
        quizOptsRow.add(qCountLbl);  quizOptsRow.add(qCountBox);

        // Hook mode switch to show/hide appropriate option rows
        modeBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                quizMode = modeBox.getSelectedIndex() == 1;
                cardOptsRow.setVisible(!quizMode);
                quizOptsRow.setVisible(quizMode);
            }
        });
        quizOptsRow.setVisible(false); // start in card-game mode

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

        final JCheckBox publicCheckFinal = publicCheck;
        createBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String room = roomField.getText().trim();
                if (room.isEmpty()) {
                    room = "ROOM" + (1000 + new Random().nextInt(8999));
                    roomField.setText(room);
                }
                doCreate(room, publicCheckFinal.isSelected());
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
        p.add(modeRow);
        p.add(Box.createVerticalStrut(4));
        p.add(cardOptsRow);
        p.add(quizOptsRow);
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

        hostStatusLabel = new JLabel("", SwingConstants.CENTER);
        hostStatusLabel.setFont(UIConstants.FONT_SMALL);
        hostStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel(
                "<html><center>Share your Room ID with friends.<br>"
                + "When ready, click <b>Start Game</b></center></html>",
                SwingConstants.CENTER);
        hint.setFont(UIConstants.FONT_SMALL);
        hint.setForeground(UIConstants.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        startBtn = makeBtn("Start Game", UIConstants.ACCENT_BLUE);
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

        updateHostStatusLabel();

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
                isHost  = false;
                myColor = "#3498DB";
                errorLabel.setText(" ");
                cardLayout.show(cardPanel, "lobby");
            }
        });

        p.add(heading);
        p.add(Box.createVerticalStrut(10));
        p.add(statusLabel);
        p.add(Box.createVerticalStrut(4));
        p.add(hostStatusLabel);
        p.add(Box.createVerticalStrut(8));
        p.add(hint);
        p.add(Box.createVerticalStrut(28));
        p.add(startBtn);
        p.add(Box.createVerticalStrut(14));
        p.add(cancelBtn);
        return p;
    }

    // ---- Helpers ----

    private void updateHostStatusLabel() {
        if (isHost) {
            hostStatusLabel.setText("YOU ARE THE HOST - Click Start to begin!");
            hostStatusLabel.setForeground(UIConstants.SUCCESS_GREEN);
            startBtn.setEnabled(true);
        } else {
            hostStatusLabel.setText("Waiting for host to start the game...");
            hostStatusLabel.setForeground(UIConstants.TEXT_MUTED);
            startBtn.setEnabled(false);
        }
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
        if (quizMode) {
            client.createQuizRoom(room, username, selectedSubject,
                    selectedQuestionCount, isPublic);
        } else {
            client.createRoom(room, username, selectedLevel, selectedCategory, isPublic);
        }
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
        final GameClient temp = new GameClient(SERVER_HOST, SERVER_PORT);
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
            // Format: room1~players~level~started~type|room2~...
            // (type: "C" = card, "Q" = quiz — older servers may omit this field)
            String[] entries = data.split("\\|");
            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                String[] parts = entry.split("~");
                if (parts.length < 4) continue;
                final String rid = parts[0];
                String players  = parts[1];
                String level    = parts[2];
                boolean started = "1".equals(parts[3]);
                String type     = parts.length >= 5 ? parts[4] : "C";
                String typeLbl  = "Q".equals(type) ? "Quiz" : "Cards L" + level;

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
                row.setOpaque(false);

                JLabel info = new JLabel(
                        rid + "   Players: " + players
                        + "   " + typeLbl
                        + (started ? "   [In Progress]" : "   [Waiting]"));
                info.setFont(UIConstants.FONT_SMALL);
                info.setForeground(started
                        ? UIConstants.TEXT_MUTED : UIConstants.TEXT_PRIMARY);

                JButton joinBtn = makeBtn("Join", UIConstants.ACCENT_PURPLE);
                joinBtn.setPreferredSize(new Dimension(70, 26));
                joinBtn.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) {
                        roomField.setText(rid);
                        doJoin(rid);
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
            isHost = true;
            quizMode = false;
            statusLabel.setText("Room created! Share Room ID: " + roomId);
            statusLabel.setForeground(UIConstants.SUCCESS_GREEN);
            updateHostStatusLabel();
            fetchRooms();

        } else if (msg.startsWith("CREATED_QUIZ:")) {
            // CREATED_QUIZ:roomId:color
            String[] p = msg.split(":");
            if (p.length >= 3) myColor = p[2];
            isHost = true;
            quizMode = true;
            statusLabel.setText("Quiz room created! Share Room ID: " + roomId);
            statusLabel.setForeground(UIConstants.SUCCESS_GREEN);
            updateHostStatusLabel();
            fetchRooms();

        } else if (msg.startsWith("JOINED:")) {
            // JOINED:roomId:color:scoreboard
            String[] p = msg.split(":", 4);
            if (p.length >= 3) myColor = p[2];
            isHost = false;
            quizMode = false;
            statusLabel.setText("Joined! Waiting for host to start...");
            statusLabel.setForeground(UIConstants.TEXT_PRIMARY);
            updateHostStatusLabel();

        } else if (msg.startsWith("JOINED_QUIZ:")) {
            // JOINED_QUIZ:roomId:color:scoreboard:subject:qCount
            String[] p = msg.split(":");
            if (p.length >= 3) myColor = p[2];
            isHost = false;
            quizMode = true;
            // Take subject + qCount from server if sent
            try {
                if (p.length >= 6) {
                    selectedSubject = Question.Subject.valueOf(p[4]);
                    totalQuestionsFromServer = Integer.parseInt(p[5]);
                }
            } catch (Exception ignored) {}
            statusLabel.setText("Joined quiz room! Waiting for host to start...");
            statusLabel.setForeground(UIConstants.TEXT_PRIMARY);
            updateHostStatusLabel();

        } else if (msg.startsWith("PLAYER_JOINED:")) {
            String[] p = msg.split(":", 4);
            String who = p.length >= 2 ? p[1] : "Someone";
            statusLabel.setText(who + " joined the room!");
            statusLabel.setForeground(UIConstants.SUCCESS_GREEN);

        } else if (msg.startsWith("GAMESTART:")) {
            // GAMESTART:rows:cols:values:scoreboard:firstTurn
            parseAndStartCardGame(msg);

        } else if (msg.startsWith("QUIZSTART:")) {
            // QUIZSTART:roomId:totalQ:scoreboard:firstTurn
            parseAndStartQuiz(msg);

        } else if (msg.startsWith("ERROR:")) {
            String err = msg.substring(6);
            statusLabel.setText("Error: " + err);
            statusLabel.setForeground(UIConstants.ERROR_RED);
            errorLabel.setText(err);
            cardLayout.show(cardPanel, "lobby");
        }
    }

    private void parseAndStartCardGame(String msg) {
        try {
            String body = msg.substring("GAMESTART:".length());
            String[] parts = body.split(":", 4);
            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            String[] values = parts[2].split(",");

            String rest = parts[3];
            int lastColon = rest.lastIndexOf(":");
            String scoreboard = lastColon >= 0 ? rest.substring(0, lastColon) : "";
            String firstTurn  = lastColon >= 0 ? rest.substring(lastColon + 1) : username;

            String color = (myColor != null && !myColor.isEmpty()) ? myColor : "#3498DB";
            listener.onGameStart(roomId, username, client,
                    rows, cols, values, selectedCategory,
                    color, scoreboard, firstTurn);

        } catch (Exception e) {
            System.err.println("[Client] Failed to parse GAMESTART: " + e.getMessage());
            errorLabel.setText("Failed to start game. Try again.");
            cardLayout.show(cardPanel, "lobby");
        }
    }

    private void parseAndStartQuiz(String msg) {
        try {
            // QUIZSTART:roomId:totalQ:scoreboard:firstTurn
            // roomId has no colons; scoreboard uses ~ and |
            String body = msg.substring("QUIZSTART:".length());
            // Split on ':' with limit 4 -> [roomId, totalQ, scoreboard, firstTurn]
            String[] parts = body.split(":", 4);
            if (parts.length < 4) throw new IllegalArgumentException("bad QUIZSTART");

            int totalQ       = Integer.parseInt(parts[1]);
            String scoreboard = parts[2];
            String firstTurn  = parts[3];

            totalQuestionsFromServer = totalQ;

            String color = (myColor != null && !myColor.isEmpty()) ? myColor : "#3498DB";
            listener.onQuizStart(roomId, username, client,
                    totalQ, color, scoreboard, firstTurn, selectedSubject);

        } catch (Exception e) {
            System.err.println("[Client] Failed to parse QUIZSTART: " + e.getMessage());
            errorLabel.setText("Failed to start quiz. Try again.");
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