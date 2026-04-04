package network;

import model.Card;

import java.io.*;
import java.net.*;

/**
 * Client-side network handler for multiplayer games.
 */
public class GameClient {

    public interface MessageListener {
        void onMessage(String message);
        void onDisconnected();
    }

    private Socket socket;
    private PrintWriter out;
    private Thread listenerThread;
    private MessageListener listener;
    private boolean connected;

    private final String host;
    private final int port;

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect(MessageListener listener) {
        this.listener = listener;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 10000);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            connected = true;
            startListening();
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Cannot connect: " + e.getMessage());
            return false;
        }
    }

    private void startListening() {
        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        final String msg = line;
                        if (listener != null) listener.onMessage(msg);
                    }
                } catch (IOException e) {
                    if (connected && listener != null) listener.onDisconnected();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // ---- Commands ----

    public void createRoom(String roomId, String username, int level,
                            Card.Category category, boolean isPublic) {
        send("CREATE:" + roomId + ":" + username + ":" + level
                + ":" + category.name() + ":" + isPublic);
    }

    public void joinRoom(String roomId, String username) {
        send("JOIN:" + roomId + ":" + username);
    }

    public void startGame(String roomId) {
        send("START:" + roomId);
    }

    public void sendFlip(String roomId, String username, int row, int col) {
        send("FLIP:" + roomId + ":" + username + ":" + row + ":" + col);
    }

    public void sendVote(String roomId, String username, boolean wantsNext) {
        send("VOTE:" + roomId + ":" + username + ":" + (wantsNext ? "yes" : "no"));
    }

    public void listPublicRooms() {
        send("LIST_ROOMS");
    }

    public void quit(String roomId, String username) {
        send("QUIT:" + roomId + ":" + username);
    }

    private synchronized void send(String message) {
        if (out != null) out.println(message);
    }

    public void setListener(MessageListener newListener) {
        this.listener = newListener;
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}