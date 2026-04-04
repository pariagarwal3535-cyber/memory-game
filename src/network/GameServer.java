package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import model.Card;
import model.GameBoard;

/**
 * Enhanced Multiplayer TCP Server
 * - Supports more than 2 players per room
 * - Turn-based system (only current player can flip)
 * - Player colors assigned on join
 * - Public rooms (joinable by anyone)
 * - Auto level progression voting
 */
public class GameServer {

    public static final int PORT;
    static {
        String p = System.getenv("PORT");
        int port = 55555;
        if (p != null && !p.isEmpty()) {
            try { port = Integer.parseInt(p.trim()); } catch (NumberFormatException e) { port = 55555; }
        }
        PORT = port;
    }

    // Player colors assigned in order
    public static final String[] PLAYER_COLORS = {
        "#E74C3C", // Red
        "#3498DB", // Blue
        "#2ECC71", // Green
        "#F39C12", // Orange
        "#9B59B6", // Purple
        "#1ABC9C", // Teal
        "#E91E63", // Pink
        "#FF5722"  // Deep Orange
    };

    // Room data
    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();
    // Public rooms list (roomId -> true)
    private final Set<String> publicRooms = ConcurrentHashMap.newKeySet();
    // All connected handlers
    private final List<ClientHandler> allClients = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private boolean running;

    // ---- Inner class to hold all room state ----
    static class RoomState {
        String roomId;
        boolean isPublic;
        int level;
        Card.Category category;
        boolean started;
        GameBoard board;

        // Players in order
        List<String> players = new ArrayList<>();
        Map<String, String> playerColors = new HashMap<>();   // username -> hex color
        Map<String, Integer> scores = new HashMap<>();        // username -> score
        Map<String, ClientHandler> handlers = new HashMap<>(); // username -> handler

        // Turn management
        int currentTurnIndex = 0;  // index into players list
        String firstFlipUser = null;
        int[] firstFlipPos = null;

        // Level vote tracking
        Map<String, Boolean> levelVotes = new HashMap<>(); // username -> wantsNext

        String getCurrentTurnPlayer() {
            if (players.isEmpty()) return null;
            return players.get(currentTurnIndex % players.size());
        }

        void nextTurn() {
            currentTurnIndex = (currentTurnIndex + 1) % players.size();
            firstFlipUser = null;
            firstFlipPos = null;
        }

        void addPlayer(String username, ClientHandler handler) {
            players.add(username);
            scores.put(username, 0);
            handlers.put(username, handler);
            // Assign color
            int colorIdx = (players.size() - 1) % PLAYER_COLORS.length;
            playerColors.put(username, PLAYER_COLORS[colorIdx]);
        }

        void removePlayer(String username) {
            players.remove(username);
            scores.remove(username);
            handlers.remove(username);
            playerColors.remove(username);
            if (currentTurnIndex >= players.size() && !players.isEmpty()) {
                currentTurnIndex = 0;
            }
        }

        String buildScoreboard() {
            StringBuilder sb = new StringBuilder();
            for (String p : players) {
                sb.append(p).append(":").append(scores.get(p))
                  .append(":").append(playerColors.get(p)).append(",");
            }
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }

    // ---- Server Start ----
    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[Server] Listening on port " + PORT);

        // Create some default public rooms
        createDefaultPublicRooms();

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                allClients.add(handler);
                new Thread(handler).start();
            } catch (SocketException e) {
                if (!running) break;
            }
        }
    }

    private void createDefaultPublicRooms() {
        // Pre-create 3 public rooms at different levels
        String[] defaultRooms = {"PUBLIC_EASY", "PUBLIC_MEDIUM", "PUBLIC_HARD"};
        int[] levels = {1, 4, 7};
        for (int i = 0; i < defaultRooms.length; i++) {
            RoomState room = new RoomState();
            room.roomId   = defaultRooms[i];
            room.isPublic = true;
            room.level    = levels[i];
            room.category = Card.Category.EMOJIS;
            room.started  = false;
            rooms.put(defaultRooms[i], room);
            publicRooms.add(defaultRooms[i]);
        }
        System.out.println("[Server] Default public rooms created.");
    }

    // ---- Room Management ----

    synchronized String createRoom(String roomId, String host, int level,
                                    Card.Category category, boolean isPublic,
                                    ClientHandler handler) {
        if (rooms.containsKey(roomId)) return "ERROR:Room ID already exists.";
        RoomState room = new RoomState();
        room.roomId   = roomId;
        room.isPublic = isPublic;
        room.level    = level;
        room.category = category;
        room.started  = false;
        room.addPlayer(host, handler);
        rooms.put(roomId, room);
        if (isPublic) publicRooms.add(roomId);
        return "CREATED:" + roomId + ":" + room.playerColors.get(host);
    }

    synchronized String joinRoom(String roomId, String username, ClientHandler handler) {
        RoomState room = rooms.get(roomId);
        if (room == null) return "ERROR:Room not found.";
        if (room.players.contains(username)) return "ERROR:Username already in room.";
        room.addPlayer(username, handler);
        String color = room.playerColors.get(username);
        // Notify others
        broadcast(roomId, "PLAYER_JOINED:" + username + ":" + color
                + ":" + room.buildScoreboard(), username);
        return "JOINED:" + roomId + ":" + color + ":" + room.buildScoreboard();
    }

    synchronized String getPublicRooms() {
        StringBuilder sb = new StringBuilder();
        for (String rid : publicRooms) {
            RoomState room = rooms.get(rid);
            if (room != null) {
                sb.append(rid).append(":").append(room.players.size())
                  .append(":").append(room.level).append(":").append(room.started).append(",");
            }
        }
        return "PUBLIC_ROOMS:" + (sb.length() > 0 ? sb.substring(0, sb.length()-1) : "");
    }

    synchronized void startGame(String roomId) {
        RoomState room = rooms.get(roomId);
        if (room == null || room.players.size() < 1) return;
        room.started  = true;
        room.board    = new GameBoard(room.level, room.category);
        room.currentTurnIndex = 0;
        room.levelVotes.clear();

        StringBuilder sb = new StringBuilder();
        sb.append("START:").append(room.board.getRows()).append(":").append(room.board.getCols()).append(":");
        for (int r = 0; r < room.board.getRows(); r++) {
            for (int c = 0; c < room.board.getCols(); c++) {
                sb.append(room.board.getCard(r, c).getValue());
                if (!(r == room.board.getRows()-1 && c == room.board.getCols()-1)) sb.append(",");
            }
        }
        sb.append(":").append(room.buildScoreboard());
        sb.append(":").append(room.getCurrentTurnPlayer());
        broadcastAll(roomId, sb.toString());
    }

    synchronized void handleFlip(String roomId, String username, int row, int col) {
        RoomState room = rooms.get(roomId);
        if (room == null || !room.started) return;

        // Block if not this player's turn
        if (!username.equals(room.getCurrentTurnPlayer())) {
            room.handlers.get(username).send("ERROR:Not your turn.");
            return;
        }

        model.Card card = room.board.getCard(row, col);
        if (card.isMatched() || card.isFlipped()) return;

        card.flip();
        broadcastAll(roomId, "FLIP_ACK:" + username + ":" + row + ":" + col + ":" + card.getValue());

        if (room.firstFlipPos == null) {
            // First flip of turn
            room.firstFlipUser = username;
            room.firstFlipPos  = new int[]{row, col};
        } else {
            // Second flip — evaluate
            model.Card firstCard  = room.board.getCard(room.firstFlipPos[0], room.firstFlipPos[1]);
            model.Card secondCard = card;

            if (firstCard.matches(secondCard)) {
                // Match!
                room.board.registerMatch(room.firstFlipPos[0], room.firstFlipPos[1], row, col);
                int newScore = room.scores.get(username) + 100;
                room.scores.put(username, newScore);
                String color = room.playerColors.get(username);

                broadcastAll(roomId, "MATCH:" + username + ":"
                        + room.firstFlipPos[0] + ":" + room.firstFlipPos[1] + ":"
                        + row + ":" + col + ":" + color + ":" + room.buildScoreboard());

                // Same player gets another turn on match
                room.firstFlipUser = null;
                room.firstFlipPos  = null;

                if (room.board.isComplete()) {
                    endLevel(roomId);
                } else {
                    broadcastAll(roomId, "TURN:" + username);
                }
            } else {
                // Miss — next player's turn
                broadcastAll(roomId, "MISS:" + username + ":"
                        + room.firstFlipPos[0] + ":" + room.firstFlipPos[1] + ":"
                        + row + ":" + col);
                room.board.getCard(room.firstFlipPos[0], room.firstFlipPos[1]).hide();
                secondCard.hide();
                room.nextTurn();
                broadcastAll(roomId, "TURN:" + room.getCurrentTurnPlayer());
            }
        }
    }

    synchronized void handleLevelVote(String roomId, String username, boolean wantsNext) {
        RoomState room = rooms.get(roomId);
        if (room == null) return;
        room.levelVotes.put(username, wantsNext);

        // Check if all players voted
        if (room.levelVotes.size() >= room.players.size()) {
            long yesVotes = room.levelVotes.values().stream().filter(v -> v).count();
            if (yesVotes > room.players.size() / 2) {
                // Majority wants next level
                room.level = Math.min(room.level + 1, 10);
                broadcastAll(roomId, "NEXT_LEVEL:" + room.level);
                startGame(roomId);
            } else {
                // Replay same level
                broadcastAll(roomId, "REPLAY_LEVEL:" + room.level);
                startGame(roomId);
            }
        } else {
            broadcastAll(roomId, "VOTE_UPDATE:" + room.levelVotes.size()
                    + ":" + room.players.size());
        }
    }

    private void endLevel(String roomId) {
        RoomState room = rooms.get(roomId);
        if (room == null) return;

        // Find winner(s)
        int maxScore = room.scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> winners = new ArrayList<>();
        for (Map.Entry<String, Integer> e : room.scores.entrySet()) {
            if (e.getValue() == maxScore) winners.add(e.getKey());
        }
        String winner = winners.size() == 1 ? winners.get(0) : "TIE";
        room.levelVotes.clear();
        broadcastAll(roomId, "LEVEL_COMPLETE:" + winner + ":" + room.buildScoreboard()
                + ":" + room.level);
    }

    void removeClient(String roomId, String username) {
        if (roomId == null || username == null) return;
        RoomState room = rooms.get(roomId);
        if (room == null) return;

        room.removePlayer(username);
        broadcastAll(roomId, "PLAYER_LEFT:" + username + ":" + room.buildScoreboard());

        // If room is empty and not a default public room, remove it
        if (room.players.isEmpty() && !isDefaultPublicRoom(roomId)) {
            rooms.remove(roomId);
            publicRooms.remove(roomId);
        } else if (room.started && !room.players.isEmpty()) {
            // Continue game, next turn
            broadcastAll(roomId, "TURN:" + room.getCurrentTurnPlayer());
        }
    }

    private boolean isDefaultPublicRoom(String roomId) {
        return roomId.equals("PUBLIC_EASY") || roomId.equals("PUBLIC_MEDIUM")
                || roomId.equals("PUBLIC_HARD");
    }

    void broadcastAll(String roomId, String message) {
        RoomState room = rooms.get(roomId);
        if (room == null) return;
        for (ClientHandler h : room.handlers.values()) h.send(message);
    }

    void broadcast(String roomId, String message, String excludeUser) {
        RoomState room = rooms.get(roomId);
        if (room == null) return;
        for (Map.Entry<String, ClientHandler> e : room.handlers.entrySet()) {
            if (!e.getKey().equals(excludeUser)) e.getValue().send(message);
        }
    }

    void removeAllClient(ClientHandler handler) {
        allClients.remove(handler);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        try { server.start(); } catch (IOException e) {
            System.err.println("[Server] Fatal: " + e.getMessage());
        }
    }
}