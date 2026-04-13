package network;

import java.io.*;
import java.net.*;
import model.Card;

/**
 * Handles one connected client on the server side.
 * Updated protocol uses GAMESTART instead of START to avoid conflicts.
 *
 * Commands from client:
 *   CREATE:roomId:username:level:category:isPublic
 *   JOIN:roomId:username
 *   STARTGAME:roomId
 *   FLIP:roomId:username:row:col
 *   VOTE:roomId:username:yes/no
 *   LIST
 *   QUIT:roomId:username
 */
public class ClientHandler implements Runnable {

    private final Socket     socket;
    private final GameServer server;
    private PrintWriter  out;
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
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) handleCommand(line);
            }
        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " + username);
        } finally {
            server.removeClient(roomId, username);
            server.removeAllClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleCommand(String cmd) {
        String[] p = cmd.split(":");
        if (p.length == 0) return;

        System.out.println("[Server<-Client] " + cmd.substring(0, Math.min(80, cmd.length())));

        switch (p[0]) {
            case "CREATE": {
                // CREATE:roomId:username:level:category:isPublic
                if (p.length < 6) { send("ERROR:Invalid CREATE"); return; }
                roomId   = p[1];
                username = p[2];
                int level = 1;
                try { level = Integer.parseInt(p[3]); } catch (Exception e) {}
                Card.Category cat = Card.Category.EMOJIS;
                try { cat = Card.Category.valueOf(p[4]); } catch (Exception e) {}
                boolean pub = "true".equals(p[5]);
                send(server.createRoom(roomId, username, level, cat, pub, this));
                break;
            }
            case "JOIN": {
                // JOIN:roomId:username
                if (p.length < 3) { send("ERROR:Invalid JOIN"); return; }
                roomId   = p[1];
                username = p[2];
                send(server.joinRoom(roomId, username, this));
                break;
            }
            case "STARTGAME": {
                // STARTGAME:roomId
                if (p.length < 2) return;
                server.startGame(p[1]);
                break;
            }
            case "FLIP": {
                // FLIP:roomId:username:row:col
                if (p.length < 5) return;
                try {
                    server.handleFlip(p[1], p[2],
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]));
                } catch (NumberFormatException e) {}
                break;
            }
            case "VOTE": {
                // VOTE:roomId:username:yes/no
                if (p.length < 4) return;
                server.handleVote(p[1], p[2], "yes".equals(p[3]));
                break;
            }
            case "LIST": {
                send(server.listRooms());
                break;
            }
            case "QUIT": {
                if (p.length >= 3)
                    server.removeClient(p[1], p[2]);
                break;
            }
            default:
                System.out.println("[Server] Unknown command: " + p[0]);
        }
    }

    public synchronized void send(String msg) {
        if (out != null) {
            out.println(msg);
            System.out.println("[Server->Client] " + msg.substring(0, Math.min(80, msg.length())));
        }
    }
}