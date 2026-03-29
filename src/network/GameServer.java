package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import model.Card;
import model.GameBoard;
import model.MultiplayerRoom;

/**
 * TCP server that hosts multiplayer Memory Game sessions.
 * Each room has two clients. The server synchronizes card flips
 * and broadcasts state updates to both players.
 *
 * Protocol (text-based, newline-delimited):
 *   Client to Server:
 *     CREATE:<roomId>:<username>:<level>:<category>
 *     JOIN:<roomId>:<username>
 *     FLIP:<roomId>:<username>:<row>:<col>
 *     QUIT:<roomId>:<username>
 *
 *   Server to Client:
 *     CREATED:<roomId>
 *     JOINED:<roomId>:<hostUsername>
 *     START:<rows>:<cols>:<boardData>   (boardData = comma-separated values)
 *     FLIP_ACK:<username>:<row>:<col>:<value>
 *     MATCH:<username>:<r1>:<c1>:<r2>:<c2>:<score>
 *     MISS:<username>:<r1>:<c1>:<r2>:<c2>
 *     SCORE_UPDATE:<username>:<score>
 *     GAME_OVER:<winner>:<score1>:<time1>:<score2>:<time2>
 *     ERROR:<message>
 */
public class GameServer {

    public static final int PORT;
static {
    String p = System.getenv("PORT");
    PORT = (p != null) ? Integer.parseInt(p) : 55555;
}

    // roomId → room data
    private final Map<String, MultiplayerRoom> rooms = new ConcurrentHashMap<>();
    // roomId → list of client handlers (max 2)
    private final Map<String, List<ClientHandler>> roomClients = new ConcurrentHashMap<>();
    // roomId → shared GameBoard
    private final Map<String, GameBoard> roomBoards = new ConcurrentHashMap<>();
    // roomId → player scores
    private final Map<String, Map<String, int[]>> roomScores = new ConcurrentHashMap<>(); // score[0]=score, score[1]=time
    // roomId → first flip pending
    private final Map<String, int[]> pendingFlip = new ConcurrentHashMap<>();  // {row,col}
    private final Map<String, String> pendingFlipper = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private boolean running;

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[Server] Listening on port " + PORT);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            } catch (SocketException e) {
                if (!running) break;
            }
        }
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    // ---- Room Management ----

    synchronized String createRoom(String roomId, String host, int level, Card.Category category, ClientHandler handler) {
        if (rooms.containsKey(roomId)) return "ERROR:Room ID already exists.";
        MultiplayerRoom room = new MultiplayerRoom(roomId, host, level, category);
        rooms.put(roomId, room);
        List<ClientHandler> list = new ArrayList<>();
        list.add(handler);
        roomClients.put(roomId, list);
        Map<String, int[]> scores = new HashMap<>();
        scores.put(host, new int[]{0, 0});
        roomScores.put(roomId, scores);
        return "CREATED:" + roomId;
    }

    synchronized String joinRoom(String roomId, String guest, ClientHandler handler) {
        MultiplayerRoom room = rooms.get(roomId);
        if (room == null)     return "ERROR:Room not found.";
        if (room.isFull())    return "ERROR:Room is full.";
        room.setGuestPlayer(guest);
        roomClients.get(roomId).add(handler);
        roomScores.get(roomId).put(guest, new int[]{0, 0});
        return "JOINED:" + roomId + ":" + room.getHostPlayer();
    }

    synchronized void startGame(String roomId) {
        MultiplayerRoom room = rooms.get(roomId);
        if (room == null || !room.isFull()) return;
        room.setStarted(true);

        GameBoard board = new GameBoard(room.getLevel(), room.getCategory());
        roomBoards.put(roomId, board);

        // Serialize board for transmission
        StringBuilder sb = new StringBuilder();
        sb.append("START:").append(board.getRows()).append(":").append(board.getCols()).append(":");
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                sb.append(board.getCard(r, c).getValue());
                if (!(r == board.getRows()-1 && c == board.getCols()-1)) sb.append(",");
            }
        }
        broadcast(roomId, sb.toString());
    }

    synchronized void handleFlip(String roomId, String username, int row, int col, ClientHandler handler) {
        GameBoard board = roomBoards.get(roomId);
        if (board == null) return;
        model.Card card = board.getCard(row, col);
        if (card.isMatched() || card.isFlipped()) return;

        card.flip();
        broadcast(roomId, "FLIP_ACK:" + username + ":" + row + ":" + col + ":" + card.getValue());

        if (!pendingFlip.containsKey(roomId)) {
            // First card
            pendingFlip.put(roomId, new int[]{row, col});
            pendingFlipper.put(roomId, username);
        } else {
            // Second card — evaluate
            int[] first = pendingFlip.remove(roomId);
            String flipper = pendingFlipper.remove(roomId);

            model.Card firstCard  = board.getCard(first[0], first[1]);
            model.Card secondCard = card;

            if (firstCard.matches(secondCard)) {
                board.registerMatch(first[0], first[1], row, col);
                Map<String, int[]> scores = roomScores.get(roomId);
                scores.get(username)[0] += 100; // award to the player who clicked second (completed the pair)
                broadcast(roomId, "MATCH:" + username + ":" + first[0] + ":" + first[1]
                        + ":" + row + ":" + col + ":" + scores.get(username)[0]);

                if (board.isComplete()) {
                    endGame(roomId);
                }
            } else {
                // Schedule hide after delay by notifying clients
                broadcast(roomId, "MISS:" + username + ":" + first[0] + ":" + first[1] + ":" + row + ":" + col);
                board.getCard(first[0], first[1]).hide();
                secondCard.hide();
            }
        }
    }

    private void endGame(String roomId) {
        MultiplayerRoom room = rooms.get(roomId);
        Map<String, int[]> scores = roomScores.get(roomId);
        String host  = room.getHostPlayer();
        String guest = room.getGuestPlayer();
        int hostScore  = scores.get(host)[0];
        int guestScore = scores.get(guest)[0];

        String winner;
        if (hostScore > guestScore)       winner = host;
        else if (guestScore > hostScore)  winner = guest;
        else winner = "TIE";

        broadcast(roomId, "GAME_OVER:" + winner + ":" + host + ":" + hostScore
                + ":" + guest + ":" + guestScore);

        // Cleanup
        rooms.remove(roomId);
        roomBoards.remove(roomId);
        roomClients.remove(roomId);
        roomScores.remove(roomId);
    }

    void removeClient(String roomId, String username) {
        if (roomId == null) return;
        List<ClientHandler> clients = roomClients.get(roomId);
        if (clients != null) broadcast(roomId, "ERROR:" + username + " disconnected.");
    }

    void broadcast(String roomId, String message) {
        List<ClientHandler> clients = roomClients.get(roomId);
        if (clients == null) return;
        for (ClientHandler c : clients) c.send(message);
    }

    // ---- Entry point for standalone server ----
    public static void main(String[] args) {
        GameServer server = new GameServer();
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[Server] Fatal: " + e.getMessage());
        }
    }
}