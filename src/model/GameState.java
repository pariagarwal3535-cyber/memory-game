package model;

/**
 * Holds the full state of a game session for one player.
 * Tracks score, time, current level, and flip attempts.
 */
public class GameState {

    private String playerName;
    private int score;
    private int level;
    private long startTime;       // System.currentTimeMillis() when level started
    private long elapsedSeconds;  // Total seconds taken for this level
    private int attempts;         // Total flip attempts this level
    private Card firstFlipped;    // First card flipped in current turn
    private boolean waitingForSecond; // Mid-turn (one card already flipped)?
    private boolean levelComplete;
    private Card.Category selectedCategory;

    public GameState(String playerName, int level, Card.Category category) {
        this.playerName = playerName;
        this.score = 0;
        this.level = level;
        this.selectedCategory = category;
        this.attempts = 0;
        this.firstFlipped = null;
        this.waitingForSecond = false;
        this.levelComplete = false;
        this.startTime = System.currentTimeMillis();
    }

    // --- Timing ---
    public void startTimer() { startTime = System.currentTimeMillis(); }

    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public void freezeTime() {
        elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
    }

    public long getFrozenElapsedSeconds() { return elapsedSeconds; }

    // --- Score logic ---
    /** Called when a successful match is made */
    public void registerMatch() {
        int timeBonus = Math.max(0, 10 - (int) getElapsedSeconds() / 10);
        score += 100 + timeBonus;
    }

    /** Deduct points for a failed attempt */
    public void registerMiss() {
        score = Math.max(0, score - 10);
    }

    // --- Flip state machine ---
    public Card getFirstFlipped() { return firstFlipped; }
    public void setFirstFlipped(Card card) {
        firstFlipped = card;
        waitingForSecond = (card != null);
    }
    public boolean isWaitingForSecond() { return waitingForSecond; }
    public void clearFirstFlipped() {
        firstFlipped = null;
        waitingForSecond = false;
    }

    // --- Getters / Setters ---
    public String getPlayerName() { return playerName; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getAttempts() { return attempts; }
    public void incrementAttempts() { attempts++; }
    public boolean isLevelComplete() { return levelComplete; }
    public void setLevelComplete(boolean levelComplete) { this.levelComplete = levelComplete; }
    public Card.Category getSelectedCategory() { return selectedCategory; }
    public void setSelectedCategory(Card.Category category) { this.selectedCategory = category; }

    @Override
    public String toString() {
        return "GameState{player='" + playerName + "', score=" + score
                + ", level=" + level + ", time=" + getElapsedSeconds() + "s}";
    }
}
