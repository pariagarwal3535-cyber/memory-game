package network;

import model.Card;
import model.GameBoard;
import quiz.Question;
import quiz.QuizBank;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Multiplayer TCP Server - now with quiz mode support.
 *
 * Card game flow is unchanged.
 *
 * Quiz flow per question:
 *   1. Server sends QUESTION to all; only primaryUser can answer (phase 1, 15s).
 *   2. If primary is correct -> +5, move to next question.
 *   3. If primary is wrong or times out -> -2 (only if answered) and server
 *      opens phase 2 (steal). Other players buzz; first BUZZ wins the floor
 *      for 10s. Correct steal +5, wrong steal -2. Primary is excluded.
 *   4. Turn rotates each question regardless of outcome.
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

    // ---- Scoring ----
    private static final int SCORE_CORRECT = 5;
    private static final int SCORE_WRONG   = -2;
    private static final int PHASE1_SECONDS = 15;
    private static final int PHASE2_SECONDS = 10;

    private final Map<String, RoomState> rooms =
            new ConcurrentHashMap<String, RoomState>();
    private final List<ClientHandler> allClients =
            new CopyOnWriteArrayList<ClientHandler>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4);

    private ServerSocket serverSocket;
    private boolean running;

    // ---- Room State ----
    static class RoomState {
        String roomId;
        String hostPlayer;
        boolean isPublic;
        boolean started;

        // Card-game fields (kept for existing card mode)
        int level;
        Card.Category category;
        GameBoard board;
        String firstFlipUser = null;
        int[]  firstFlipPos  = null;

        // Quiz-game fields
        boolean quizRoom = false;
        Question.Subject subject;
        int questionCount = 10;
        List<Question> questions = new ArrayList<Question>();
        int currentQIndex = -1;
        int currentPhase  = 0;   // 0 = between questions, 1 = primary, 2 = steal
        boolean questionResolved = false; // once true, no more phase transitions
        String currentBuzzUser = null;   // whoever has the floor in phase 2
        Set<String> phase2Answered = new HashSet<String>(); // not used for fair steal
        ScheduledFuture<?> currentTimer;

        List<String> players           = new ArrayList<String>();
        Map<String, String>  colors    = new HashMap<String, String>();
        Map<String, Integer> scores    = new HashMap<String, Integer>();
        Map<String, ClientHandler> handlers = new HashMap<String, ClientHandler>();
        Map<String, Boolean> votes     = new HashMap<String, Boolean>();

        int turnIndex = 0;

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

        Question currentQuestion() {
            if (currentQIndex < 0 || currentQIndex >= questions.size()) return null;
            return questions.get(currentQIndex);
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

    // ---- Room Operations (card game) ----

    synchronized String createRoom(String roomId, String host, int level,
                                    Card.Category cat, boolean isPublic,
                                    ClientHandler handler) {
        if (rooms.containsKey(roomId))
            return "ERROR:Room already exists";
        RoomState r = new RoomState();
        r.roomId      = roomId;
        r.hostPlayer  = host;
        r.isPublic    = isPublic;
        r.level       = level;
        r.category    = cat;
        r.started     = false;
        r.addPlayer(host, handler);
        rooms.put(roomId, r);
        System.out.println("[Server] Card room created: " + roomId + " by " + host);
        return "CREATED:" + roomId + ":" + r.colors.get(host);
    }

    // ---- Quiz room creation ----

    synchronized String createQuizRoom(String roomId, String host,
                                        Question.Subject subject, int qCount,
                                        boolean isPublic, ClientHandler handler) {
        if (rooms.containsKey(roomId))
            return "ERROR:Room already exists";
        RoomState r = new RoomState();
        r.roomId      = roomId;
        r.hostPlayer  = host;
        r.isPublic    = isPublic;
        r.started     = false;
        r.quizRoom    = true;
        r.subject     = subject;
        r.questionCount = Math.max(3, Math.min(qCount, 20));
        r.addPlayer(host, handler);
        rooms.put(roomId, r);
        System.out.println("[Server] Quiz room created: " + roomId + " by " + host
                + " subject=" + subject + " qCount=" + r.questionCount);
        // CREATED_QUIZ:roomId:color
        return "CREATED_QUIZ:" + roomId + ":" + r.colors.get(host);
    }

    synchronized String joinRoom(String roomId, String user, ClientHandler handler) {
        RoomState r = rooms.get(roomId);
        if (r == null)              return "ERROR:Room not found";
        if (r.players.contains(user)) return "ERROR:Username taken in this room";
        r.addPlayer(user, handler);
        String color = r.colors.get(user);
        System.out.println("[Server] " + user + " joined room " + roomId);
        broadcastExcept(roomId, "PLAYER_JOINED:" + user + ":" + color
                + ":" + r.scoreboard(), user);
        if (r.quizRoom) {
            return "JOINED_QUIZ:" + roomId + ":" + color + ":" + r.scoreboard()
                    + ":" + r.subject.name() + ":" + r.questionCount;
        }
        return "JOINED:" + roomId + ":" + color + ":" + r.scoreboard();
    }

    synchronized String listRooms() {
        StringBuilder sb = new StringBuilder();
        for (RoomState r : rooms.values()) {
            if (sb.length() > 0) sb.append("|");
            sb.append(r.roomId).append("~")
              .append(r.players.size()).append("~")
              .append(r.level).append("~")
              .append(r.started ? "1" : "0").append("~")
              .append(r.quizRoom ? "Q" : "C");
        }
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
        if (!requester.equals(r.hostPlayer)) {
            System.out.println("[Server] startGame blocked: " + requester
                    + " is not host (" + r.hostPlayer + ") of " + roomId);
            ClientHandler h = r.handlers.get(requester);
            if (h != null) h.send("ERROR:Only the host can start the game");
            return;
        }

        if (r.quizRoom) {
            startQuizGame(r);
        } else {
            startCardGame(r);
        }
    }

    private void startCardGame(RoomState r) {
        r.started   = true;
        r.turnIndex = 0;
        r.votes.clear();
        r.board     = new GameBoard(r.level, r.category);
        for (String p : r.players) r.scores.put(p, 0);

        System.out.println("[Server] Card game starting in " + r.roomId
                + " with " + r.players.size() + " players");

        StringBuilder vals = new StringBuilder();
        for (int row = 0; row < r.board.getRows(); row++) {
            for (int col = 0; col < r.board.getCols(); col++) {
                if (vals.length() > 0) vals.append(",");
                vals.append(r.board.getCard(row, col).getValue());
            }
        }

        String msg = "GAMESTART:" + r.board.getRows()
                + ":" + r.board.getCols()
                + ":" + vals.toString()
                + ":" + r.scoreboard()
                + ":" + r.currentTurn();
        broadcastAll(r.roomId, msg);
    }

    private void startQuizGame(RoomState r) {
        r.started   = true;
        r.turnIndex = 0;
        for (String p : r.players) r.scores.put(p, 0);

        // Pick N random questions from the subject
        List<Question> picked = QuizBank.getRandom(r.subject, r.questionCount);
        r.questions = picked;
        r.currentQIndex = -1;

        System.out.println("[Server] Quiz starting in " + r.roomId
                + " (" + r.questions.size() + " questions, subject=" + r.subject + ")");

        // QUIZSTART:roomId:totalQuestions:scoreboard:firstTurn
        broadcastAll(r.roomId, "QUIZSTART:" + r.roomId
                + ":" + r.questions.size()
                + ":" + r.scoreboard()
                + ":" + r.currentTurn());

        // Send the first question after a short delay so clients can swap UI
        scheduler.schedule(new Runnable() {
            @Override public void run() { nextQuestion(r.roomId); }
        }, 600, TimeUnit.MILLISECONDS);
    }

    // ---- Card game handlers (unchanged behavior) ----

    synchronized void handleFlip(String roomId, String user, int row, int col) {
        RoomState r = rooms.get(roomId);
        if (r == null || !r.started || r.quizRoom) return;

        if (!user.equals(r.currentTurn())) {
            ClientHandler h = r.handlers.get(user);
            if (h != null) h.send("BLOCKED:Not your turn");
            return;
        }

        if (row < 0 || row >= r.board.getRows() || col < 0 || col >= r.board.getCols()) {
            ClientHandler h = r.handlers.get(user);
            if (h != null) h.send("ERROR:Invalid card position");
            return;
        }

        Card card = r.board.getCard(row, col);
        if (card == null || card.isMatched() || card.isFlipped()) return;

        card.flip();
        String cardVal = card.getValue() != null ? card.getValue() : "unknown";
        broadcastAll(roomId, "FLIP:" + user + ":" + row + ":" + col + ":" + cardVal);

        if (r.firstFlipPos == null) {
            r.firstFlipUser = user;
            r.firstFlipPos  = new int[]{row, col};
        } else {
            Card first  = r.board.getCard(r.firstFlipPos[0], r.firstFlipPos[1]);
            Card second = card;

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
            startGame(roomId, r.hostPlayer);
        } else {
            broadcastAll(roomId,
                    "VOTEUPDATE:" + r.votes.size() + ":" + r.players.size());
        }
    }

    // ---- Quiz handlers ----

    private synchronized void nextQuestion(String roomId) {
        RoomState r = rooms.get(roomId);
        if (r == null || !r.started || !r.quizRoom) return;

        r.currentQIndex++;
        if (r.currentQIndex >= r.questions.size()) {
            endQuiz(r);
            return;
        }
        r.currentPhase = 1;
        r.questionResolved = false;
        r.currentBuzzUser = null;
        r.phase2Answered.clear();

        Question q = r.currentQuestion();
        String primary = r.currentTurn();

        // QUESTION:qIdx:totalQ:primaryUser:question:opt0|opt1|opt2|opt3:timeSec
        String optsJoined = q.getOptions()[0] + "|" + q.getOptions()[1]
                + "|" + q.getOptions()[2] + "|" + q.getOptions()[3];
        broadcastAll(roomId, "QUESTION:" + r.currentQIndex
                + ":" + r.questions.size()
                + ":" + primary
                + ":" + q.getQuestion()
                + ":" + optsJoined
                + ":" + PHASE1_SECONDS);

        scheduleTimeout(r, PHASE1_SECONDS, 1, r.currentQIndex);
    }

    private void scheduleTimeout(final RoomState r, int seconds,
                                  final int phase, final int qIdxSnapshot) {
        if (r.currentTimer != null) r.currentTimer.cancel(false);
        final String roomId = r.roomId;
        r.currentTimer = scheduler.schedule(new Runnable() {
            @Override public void run() {
                onPhaseTimeout(roomId, phase, qIdxSnapshot);
            }
        }, seconds, TimeUnit.SECONDS);
    }

    private synchronized void onPhaseTimeout(String roomId, int phase, int qIdxSnapshot) {
        RoomState r = rooms.get(roomId);
        if (r == null || !r.started || !r.quizRoom) return;
        // Only act if question hasn't moved on
        if (r.currentQIndex != qIdxSnapshot) return;
        if (r.currentPhase != phase) return;
        // HARD GUARD: if question has been resolved by an answer, never fire phase2
        if (r.questionResolved) return;

        Question q = r.currentQuestion();
        if (q == null) return;

        if (phase == 1) {
            // Primary timed out - no score change (only answering gives -2)
            // Open to steal if more than 1 player exists
            if (r.players.size() <= 1) {
                // Solo mode edge case - skip
                advanceAfterQuestion(r, q.getCorrectIndex());
                return;
            }
            broadcastAll(roomId, "PHASE2:" + r.currentQIndex
                    + ":" + PHASE2_SECONDS);
            r.currentPhase = 2;
            r.currentBuzzUser = null;
            scheduleTimeout(r, PHASE2_SECONDS, 2, r.currentQIndex);

        } else if (phase == 2) {
            if (r.currentBuzzUser == null) {
                // Nobody buzzed - just reveal and move on
                advanceAfterQuestion(r, q.getCorrectIndex());
            } else {
                // Buzzer user timed out on their answer - treat as wrong
                applyScore(r, r.currentBuzzUser, SCORE_WRONG);
                broadcastAll(roomId, "ANSWERED:" + r.currentBuzzUser
                        + ":-1:0:" + r.scoreboard()
                        + ":" + q.getCorrectIndex());
                advanceAfterQuestion(r, q.getCorrectIndex());
            }
        }
    }

    synchronized void handleAnswer(String roomId, String user, int optIdx) {
        RoomState r = rooms.get(roomId);
        if (r == null || !r.started || !r.quizRoom) return;
        Question q = r.currentQuestion();
        if (q == null) return;

        if (r.currentPhase == 1) {
            // Only the primary can answer in phase 1
            if (!user.equals(r.currentTurn())) {
                ClientHandler h = r.handlers.get(user);
                if (h != null) h.send("BLOCKED:Not your question");
                return;
            }
            boolean correct = (optIdx == q.getCorrectIndex());
            if (correct) {
                // Mark resolved FIRST so a racing timeout can't fire phase2
                r.questionResolved = true;
                if (r.currentTimer != null) { r.currentTimer.cancel(false); r.currentTimer = null; }
                applyScore(r, user, SCORE_CORRECT);
                broadcastAll(roomId, "ANSWERED:" + user + ":" + optIdx + ":1:"
                        + r.scoreboard() + ":" + q.getCorrectIndex());
                advanceAfterQuestion(r, q.getCorrectIndex());
            } else {
                applyScore(r, user, SCORE_WRONG);
                broadcastAll(roomId, "ANSWERED:" + user + ":" + optIdx + ":0:"
                        + r.scoreboard() + ":" + q.getCorrectIndex());
                // Open steal round
                if (r.players.size() <= 1) {
                    r.questionResolved = true;
                    advanceAfterQuestion(r, q.getCorrectIndex());
                    return;
                }
                broadcastAll(roomId, "PHASE2:" + r.currentQIndex
                        + ":" + PHASE2_SECONDS);
                r.currentPhase = 2;
                r.currentBuzzUser = null;
                scheduleTimeout(r, PHASE2_SECONDS, 2, r.currentQIndex);
            }

        } else if (r.currentPhase == 2) {
            // Only the buzzer can answer
            if (r.currentBuzzUser == null || !user.equals(r.currentBuzzUser)) {
                ClientHandler h = r.handlers.get(user);
                if (h != null) h.send("BLOCKED:Not your buzz");
                return;
            }
            // Mark resolved FIRST to block the timeout from firing again
            r.questionResolved = true;
            if (r.currentTimer != null) { r.currentTimer.cancel(false); r.currentTimer = null; }
            boolean correct = (optIdx == q.getCorrectIndex());
            if (correct) applyScore(r, user, SCORE_CORRECT);
            else         applyScore(r, user, SCORE_WRONG);

            broadcastAll(roomId, "ANSWERED:" + user + ":" + optIdx
                    + ":" + (correct ? 1 : 0)
                    + ":" + r.scoreboard()
                    + ":" + q.getCorrectIndex());
            advanceAfterQuestion(r, q.getCorrectIndex());
        }
    }

    synchronized void handleBuzz(String roomId, String user) {
        RoomState r = rooms.get(roomId);
        if (r == null || !r.started || !r.quizRoom) return;
        if (r.currentPhase != 2) return;
        // Primary player excluded from steal round
        if (user.equals(r.currentTurn())) {
            ClientHandler h = r.handlers.get(user);
            if (h != null) h.send("BLOCKED:You cannot buzz your own question");
            return;
        }
        // First buzz wins
        if (r.currentBuzzUser != null) return;
        r.currentBuzzUser = user;

        // BUZZED:user:timeSec
        broadcastAll(roomId, "BUZZED:" + user + ":" + PHASE2_SECONDS);
        // Reset timer for the buzzer's answer window
        scheduleTimeout(r, PHASE2_SECONDS, 2, r.currentQIndex);
    }

    private void applyScore(RoomState r, String user, int delta) {
        Integer cur = r.scores.get(user);
        if (cur == null) cur = 0;
        r.scores.put(user, cur + delta);
    }

    private void advanceAfterQuestion(RoomState r, int correctIdx) {
        if (r.currentTimer != null) { r.currentTimer.cancel(false); r.currentTimer = null; }
        // Rotate turn for every question outcome
        r.nextTurn();
        r.currentPhase = 0;
        r.currentBuzzUser = null;

        final String roomId = r.roomId;
        scheduler.schedule(new Runnable() {
            @Override public void run() { nextQuestion(roomId); }
        }, 1200, TimeUnit.MILLISECONDS);
    }

    private void endQuiz(RoomState r) {
        r.started = false;
        int max = Integer.MIN_VALUE;
        for (int s : r.scores.values()) if (s > max) max = s;
        List<String> winners = new ArrayList<String>();
        for (Map.Entry<String, Integer> e : r.scores.entrySet())
            if (e.getValue() == max) winners.add(e.getKey());
        String winner = winners.size() == 1 ? winners.get(0) : "TIE";
        broadcastAll(r.roomId, "QUIZEND:" + winner + ":" + r.scoreboard());
    }

    // ---- Generic helpers ----

    void removeClient(String roomId, String user) {
        if (roomId == null || user == null) return;
        RoomState r = rooms.get(roomId);
        if (r == null) return;
        r.removePlayer(user);
        broadcastAll(roomId, "PLAYERLEFT:" + user + ":" + r.scoreboard());
        if (r.players.isEmpty() && !isDefaultRoom(roomId)) {
            if (r.currentTimer != null) r.currentTimer.cancel(false);
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
        System.out.println("[Server->Room " + roomId + "] " + msg.substring(0, Math.min(80, msg.length())));
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
        scheduler.shutdownNow();
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        try { server.start(); }
        catch (IOException e) {
            System.err.println("[Server] Fatal: " + e.getMessage());
        }
    }
}