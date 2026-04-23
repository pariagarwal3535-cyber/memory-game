package network;

import model.Card;
import quiz.Question;

import java.io.*;
import java.net.*;

/**
 * Client-side socket handler.
 * Now supports both card-game and quiz-game commands.
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
    private boolean connected = false;

    private final String host;
    private final int    port;

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
            System.err.println("[Client] Cannot connect to " + host + ":" + port
                    + " - " + e.getMessage());
            return false;
        }
    }

    private void startListening() {
        listenerThread = new Thread(new Runnable() {
            @Override public void run() {
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

    // ---- Card game commands ----

    public void createRoom(String roomId, String user, int level,
                            Card.Category cat, boolean isPublic) {
        send("CREATE:" + roomId + ":" + user + ":" + level
                + ":" + cat.name() + ":" + isPublic);
    }

    public void sendFlip(String roomId, String user, int row, int col) {
        send("FLIP:" + roomId + ":" + user + ":" + row + ":" + col);
    }

    public void sendVote(String roomId, String user, boolean next) {
        send("VOTE:" + roomId + ":" + user + ":" + (next ? "yes" : "no"));
    }

    // ---- Quiz game commands ----

    public void createQuizRoom(String roomId, String user,
                                Question.Subject subject, int questionCount,
                                boolean isPublic) {
        send("CREATE_QUIZ:" + roomId + ":" + user
                + ":" + subject.name()
                + ":" + questionCount
                + ":" + isPublic);
    }

    public void sendAnswer(String roomId, String user, int optionIndex) {
        send("ANSWER:" + roomId + ":" + user + ":" + optionIndex);
    }

    public void sendBuzz(String roomId, String user) {
        send("BUZZ:" + roomId + ":" + user);
    }

    // ---- Shared commands ----

    public void joinRoom(String roomId, String user) {
        send("JOIN:" + roomId + ":" + user);
    }

    public void startGame(String roomId) {
        send("STARTGAME:" + roomId);
    }

    public void listRooms() { send("LIST"); }

    public void quit(String roomId, String user) {
        send("QUIT:" + roomId + ":" + user);
    }

    private synchronized void send(String msg) {
        if (out != null) out.println(msg);
    }

    public void setListener(MessageListener l) { this.listener = l; }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); }
        catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}