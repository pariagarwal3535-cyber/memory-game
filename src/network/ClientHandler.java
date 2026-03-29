package network;

import model.Card;

import java.io.*;
import java.net.*;

/**
 * Handles one connected client on the server side.
 * Reads commands in a loop and delegates to GameServer.
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
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            String line;
            while ((line = in.readLine()) != null) {
                handleCommand(line.trim());
            }
        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " + username);
        } finally {
            server.removeClient(roomId, username);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleCommand(String command) {
        String[] parts = command.split(":");

        switch (parts[0]) {
            case "CREATE": {
                // CREATE:<roomId>:<username>:<level>:<category>
                if (parts.length < 5) { send("ERROR:Invalid CREATE command."); return; }
                roomId   = parts[1];
                username = parts[2];
                int level = Integer.parseInt(parts[3]);
                Card.Category cat = Card.Category.valueOf(parts[4]);
                String response = server.createRoom(roomId, username, level, cat, this);
                send(response);
                break;
            }
            case "JOIN": {
                // JOIN:<roomId>:<username>
                if (parts.length < 3) { send("ERROR:Invalid JOIN command."); return; }
                roomId   = parts[1];
                username = parts[2];
                String response = server.joinRoom(roomId, username, this);
                send(response);
                if (response.startsWith("JOINED")) {
                    // Notify host and start game
                    server.broadcast(roomId, "PLAYER_JOINED:" + username);
                    server.startGame(roomId);
                }
                break;
            }
            case "FLIP": {
                // FLIP:<roomId>:<username>:<row>:<col>
                if (parts.length < 5) { send("ERROR:Invalid FLIP command."); return; }
                String rid  = parts[1];
                String uname = parts[2];
                int row = Integer.parseInt(parts[3]);
                int col = Integer.parseInt(parts[4]);
                server.handleFlip(rid, uname, row, col, this);
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

    /** Thread-safe send */
    public synchronized void send(String message) {
        if (out != null) out.println(message);
    }
}
