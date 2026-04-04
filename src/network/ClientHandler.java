package network;

import java.io.*;
import java.net.*;
import model.Card;

/**
 * Handles one connected client on the server side.
 * Updated protocol:
 *   CREATE:<roomId>:<username>:<level>:<category>:<isPublic>
 *   JOIN:<roomId>:<username>
 *   START:<roomId>
 *   FLIP:<roomId>:<username>:<row>:<col>
 *   VOTE:<roomId>:<username>:<yes/no>
 *   LIST_ROOMS
 *   QUIT:<roomId>:<username>
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameServer server;
    private PrintWriter out;
    private String username;
    private String roomId;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            String line;
            while ((line = in.readLine()) != null) {
                handleCommand(line.trim());
            }
        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " + username);
        } finally {
            server.removeClient(roomId, username);
            server.removeAllClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleCommand(String command) {
        if (command.isEmpty()) return;
        String[] parts = command.split(":");

        switch (parts[0]) {
            case "CREATE": {
                // CREATE:<roomId>:<username>:<level>:<category>:<isPublic>
                if (parts.length < 6) { send("ERROR:Invalid CREATE"); return; }
                roomId   = parts[1];
                username = parts[2];
                int level = Integer.parseInt(parts[3]);
                Card.Category cat = Card.Category.valueOf(parts[4]);
                boolean isPublic = Boolean.parseBoolean(parts[5]);
                String response = server.createRoom(roomId, username, level, cat, isPublic, this);
                send(response);
                break;
            }
            case "JOIN": {
                // JOIN:<roomId>:<username>
                if (parts.length < 3) { send("ERROR:Invalid JOIN"); return; }
                roomId   = parts[1];
                username = parts[2];
                String response = server.joinRoom(roomId, username, this);
                send(response);
                break;
            }
            case "START": {
                // START:<roomId>
                if (parts.length < 2) return;
                server.startGame(parts[1]);
                break;
            }
            case "FLIP": {
                // FLIP:<roomId>:<username>:<row>:<col>
                if (parts.length < 5) return;
                server.handleFlip(parts[1], parts[2],
                        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                break;
            }
            case "VOTE": {
                // VOTE:<roomId>:<username>:<yes/no>
                if (parts.length < 4) return;
                server.handleLevelVote(parts[1], parts[2], parts[3].equals("yes"));
                break;
            }
            case "LIST_ROOMS": {
                send(server.getPublicRooms());
                break;
            }
            case "QUIT": {
                server.removeClient(roomId, username);
                break;
            }
            default:
                send("ERROR:Unknown command: " + parts[0]);
        }
    }

    public synchronized void send(String message) {
        if (out != null) out.println(message);
    }
}