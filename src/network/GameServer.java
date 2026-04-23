package network;

import model.Card;
import model.GameBoard;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simplified Multiplayer TCP Server
 * Fixed protocol - no colons in scoreboard during START
 * Rooms auto-start when host clicks start
 */
public class GameServer {

    public static final int PORT;
    static {
        String p = System.getenv("PORT");
        int port = 55555;
        if (p != null && !p.isEmpty()) {
            try { port = Integer.parseInt(p.trim()); }
            catch (NumberFormatException e) { port = 55555; }
        }
        PORT = port;
    }

    public static final String[] PLAYER_COLORS = {
        "#E74C3C", "#3498DB", "#2ECC71", "#F39C12",
        "#9B59B6", "#1ABC9C", "#E91E63", "#FF5722"
    };

    private final Map<String, RoomState> rooms =
            new ConcurrentHashMap<String, RoomState>();
    private final List<ClientHandler> allClients =
            new CopyOnWriteArrayList<ClientHandler>();

    private ServerSocket serverSocket;
    private boolean running;

    // ---- Room State ----
    static class RoomState {
        String roomId;
        String hostPlayer;  // Store host for permission checks
        boolean isPublic;
        int level;
        Card.Category category;
        boolean started;
        GameBoard board;

        List<String> players           = new ArrayList<String>();
        Map<String, String>  colors    = new HashMap<String, String>();
        Map<String, Integer> scores    = new HashMap<String, Integer>();
        Map<String, ClientHandler> handlers = new HashMap<String, ClientHandler>();
        Map<String, Boolean> votes     = new HashMap<String, Boolean>();

        int turnIndex = 0;
        String firstFlipUser = null;
        int[]  firstFlipPos  = null;

        String currentTurn() {
            if (players.isEmpty()) return "";
            return players.get(turnIndex % players.size());
        }

        void nextTurn() {
            turnIndex = (turnIndex + 1) % players.size();
            firstFlipUser = null;
            firstFlipPos  = null;
        }

        void addPlayer(String user, ClientHandler h) {
            players.add(user);
            scores.put(user, 0);
            handlers.put(user, h);
            colors.put(user, PLAYER_COLORS[(players.size()-1) % PLAYER_COLORS.length]);
        }

        void removePlayer(String user) {
            players.remove(user);
            scores.remove(user);
            handlers.remove(user);
            colors.remove(user);
            if (!players.isEmpty())
                turnIndex = turnIndex % players.size();
        }

        // Scoreboard as pipe-separated: user|score|color~user|score|color
        // Using | and ~ to avoid colon conflicts
        String scoreboard() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < players.size(); i++) {
                String p = players.get(i);
                if (i > 0) sb.append("~");
                sb.append(p).append("|").append(scores.get(p))
                  .append("|").append(colors.get(p));
            }
            return sb.toString();
        }
    }

    // ---- Server Start ----
    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[Server] Started on port " + PORT);
        createDefaultPublicRooms();

        while (running) {
            try {
                Socket s = serverSocket.accept();
                ClientHandler h = new ClientHandler(s, this);
                allClients.add(h);
                new Thread(h).start();
                System.out.println("[Server] New client connected. Total: " + allClients.size());
            } catch (SocketException e) {
                if (!running) break;
            }
        }
    }

    private void createDefaultPublicRooms() {
        String[] ids  = {"EASY_ROOM", "MEDIUM_ROOM", "HARD_ROOM"};
        int[] levels  = {1, 4, 8};
        for (int i = 0; i < ids.length; i++) {
            RoomState r  = new RoomState();
            r.roomId     = ids[i];
            r.isPublic   = true;
            r.level      = levels[i];
            r.category   = Card.Category.EMOJIS;
            r.started    = false;
            rooms.put(ids[i], r);
        }
        System.out.println("[Server] Default public rooms created.");
    }

    // ---- Room Operations ----

    synchronized String createRoom(String roomId, String host, int level,
                                    Card.Category cat, boolean isPublic,
                                    ClientHandler handler) {
        if (rooms.containsKey(roomId))
            return "ERROR:Room already exists";
        RoomState r = new RoomState();
        r.roomId      = roomId;
        r.hostPlayer  = host;  // Set host
        r.isPublic    = isPublic;
        r.level       = level;
        r.category    = cat;
        r.started     = false;
        r.addPlayer(host, handler);
        rooms.put(roomId, r);
        System.out.println("[Server] Room created: " + roomId + " by " + host + " (HOST)");
        // CREATED:roomId:color
        return "CREATED:" + roomId + ":" + r.colors.get(host);
    }

    synchronized String joinRoom(String roomId, String user, ClientHandler handler) {
        RoomState r = rooms.get(roomId);
        if (r == null)              return "ERROR:Room not found";
        if (r.players.contains(user)) return "ERROR:Username taken in this room";
        r.addPlayer(user, handler);
        String color = r.colors.get(user);
        System.out.println("[Server] " + user + " joined room " + roomId);
        // Tell others someone joined
        broadcastExcept(roomId, "PLAYER_JOINED:" + user + ":" + color
                + ":" + r.scoreboard(), user);
        // JOINED:roomId:color:scoreboard
        return "JOINED:" + roomId + ":" + color + ":" + r.scoreboard();
    }

    synchronized String listRooms() {
        StringBuilder sb = new StringBuilder();
        for (RoomState r : rooms.values()) {
            if (sb.length() > 0) sb.append("|");
            sb.append(r.roomId).append("~")
              .append(r.players.size()).append("~")
              .append(r.level).append("~")
              .append(r.started ? "1" : "0");
        }
        // LIST:room1~players~level~started|room2~...
        return "LIST:" + sb.toString();
    }

    synchronized void startGame(String roomId, String requester) {
        RoomState r = rooms.get(roomId);
        if (r == null) {
            System.out.println("[Server] startGame failed: room not found " + roomId);
            return;
        }
        if (r.players.isEmpty()) {
            System.out.println("[Server] startGame failed: no players in " + roomId);
            return;
        }
        // CRITICAL: Only host can start the game
        if (!requester.equals(r.hostPlayer)) {
            System.out.println("[Server] startGame blocked: " + requester + " is not host (" + r.hostPlayer + ") of " + roomId);
            ClientHandler h = r.handlers.get(requester);
            if (h != null) h.send("ERROR:Only the host can start the game");
            return;
        }
        r.started   = true;
        r.turnIndex = 0;
        r.votes.clear();
        r.board     = new GameBoard(r.level, r.category);
        for (String p : r.players) r.scores.put(p, 0);

        System.out.println("[Server] Game starting in room " + roomId
                + " with " + r.players.size() + " players");

        // Build board data (values only, comma-separated)
        StringBuilder vals = new StringBuilder();
        for (int row = 0; row < r.board.getRows(); row++) {
            for (int col = 0; col < r.board.getCols(); col++) {
                if (vals.length() > 0) vals.append(",");
                vals.append(r.board.getCard(row, col).getValue());
            }
        }

        // GAMESTART:rows:cols:values:scoreboard:firstTurn
        // Use GAMESTART (not START) to avoid keyword confusion
        String msg = "GAMESTART:" + r.board.getRows()
                + ":" + r.board.getCols()
                + ":" + vals.toString()
                + ":" + r.scoreboard()
                + ":" + r.currentTurn();
        broadcastAll(roomId, msg);
        System.out.println("[Server] GAMESTART sent to room " + roomId);
    }

    synchronized void handleFlip(String roomId, String user, int row, int col) {
        RoomState r = rooms.get(roomId);
        if (r == null || !r.started) return;

        if (!user.equals(r.currentTurn())) {
            ClientHandler h = r.handlers.get(user);
            if (h != null) h.send("BLOCKED:Not your turn");
            return;
        }

        // Validate indices to prevent out-of-bounds
        if (row < 0 || row >= r.board.getRows() || col < 0 || col >= r.board.getCols()) {
            ClientHandler h = r.handlers.get(user);
            if (h != null) h.send("ERROR:Invalid card position");
            return;
        }

        model.Card card = r.board.getCard(row, col);
        // CRITICAL: Check card state before processing
        if (card == null || card.isMatched() || card.isFlipped()) {
            System.out.println("[Server] Flip rejected: card already matched/flipped at (" + row + "," + col + ")");
            return;
        }

        card.flip();
        String cardVal = card.getValue() != null ? card.getValue() : "unknown";
        broadcastAll(roomId, "FLIP:" + user + ":" + row + ":" + col
                + ":" + cardVal);

        if (r.firstFlipPos == null) {
            r.firstFlipUser = user;
            r.firstFlipPos  = new int[]{row, col};
        } else {
            model.Card first  = r.board.getCard(r.firstFlipPos[0], r.firstFlipPos[1]);
            model.Card second = card;

            if (first.matches(second)) {
                int fr = r.firstFlipPos[0], fc = r.firstFlipPos[1];
                r.board.registerMatch(fr, fc, row, col);
                r.scores.put(user, r.scores.get(user) + 100);
                r.firstFlipPos  = null;
                r.firstFlipUser = null;

                broadcastAll(roomId, "MATCH:" + user
                        + ":" + fr + ":" + fc
                        + ":" + row + ":" + col
                        + ":" + r.colors.get(user)
                        + ":" + r.scoreboard());

                if (r.board.isComplete()) {
                    endLevel(roomId);
                } else {
                    broadcastAll(roomId, "TURN:" + user);
                }
            } else {
                first.hide();
                second.hide();
                r.firstFlipPos  = null;
                r.firstFlipUser = null;
                r.nextTurn();
                broadcastAll(roomId, "MISS:" + user + ":" + r.currentTurn());
                broadcastAll(roomId, "TURN:" + r.currentTurn());
            }
        }
    }

    private void endLevel(String roomId) {
        RoomState r = rooms.get(roomId);
        if (r == null) return;
        int maxScore = 0;
        for (int s : r.scores.values()) if (s > maxScore) maxScore = s;
        List<String> winners = new ArrayList<String>();
        for (Map.Entry<String, Integer> e : r.scores.entrySet())
            if (e.getValue() == maxScore) winners.add(e.getKey());
        String winner = winners.size() == 1 ? winners.get(0) : "TIE";
        r.votes.clear();
        broadcastAll(roomId, "LEVELEND:" + winner + ":" + r.scoreboard()
                + ":" + r.level);
    }

    synchronized void handleVote(String roomId, String user, boolean next) {
        RoomState r = rooms.get(roomId);
        if (r == null) return;
        r.votes.put(user, next);
        if (r.votes.size() >= r.players.size()) {
            long yes = 0;
            for (Boolean v : r.votes.values()) if (v) yes++;
            if (yes > r.players.size() / 2) {
                r.level = Math.min(r.level + 1, 10);
                broadcastAll(roomId, "NEXTLEVEL:" + r.level);
            } else {
                broadcastAll(roomId, "REPLAYLEVEL:" + r.level);
            }
            // Auto-start the next level (initiated by server/host authority)
            startGame(roomId, r.hostPlayer);
        } else {
            broadcastAll(roomId,
                    "VOTEUPDATE:" + r.votes.size() + ":" + r.players.size());
        }
    }

    void removeClient(String roomId, String user) {
        if (roomId == null || user == null) return;
        RoomState r = rooms.get(roomId);
        if (r == null) return;
        r.removePlayer(user);
        broadcastAll(roomId, "PLAYERLEFT:" + user + ":" + r.scoreboard());
        if (r.players.isEmpty() && !isDefaultRoom(roomId)) {
            rooms.remove(roomId);
        }
    }

    void removeAllClient(ClientHandler h) { allClients.remove(h); }

    private boolean isDefaultRoom(String id) {
        return id.equals("EASY_ROOM") || id.equals("MEDIUM_ROOM")
                || id.equals("HARD_ROOM");
    }

    void broadcastAll(String roomId, String msg) {
        RoomState r = rooms.get(roomId);
        if (r == null) return;
        System.out.println("[Server->Room " + roomId + "] " + msg.substring(0, Math.min(60, msg.length())));
        for (ClientHandler h : r.handlers.values()) h.send(msg);
    }

    void broadcastExcept(String roomId, String msg, String exclude) {
        RoomState r = rooms.get(roomId);
        if (r == null) return;
        for (Map.Entry<String, ClientHandler> e : r.handlers.entrySet())
            if (!e.getKey().equals(exclude)) e.getValue().send(msg);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        try { server.start(); }
        catch (IOException e) {
            System.err.println("[Server] Fatal: " + e.getMessage());
        }
    }
}