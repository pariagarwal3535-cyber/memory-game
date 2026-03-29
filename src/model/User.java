package model;

/**
 * Represents a registered user in the Memory Game.
 * Encapsulates user credentials and basic profile info.
 */
public class User {
    private String username;
    private String password;
    private int highScore;
    private int gamesPlayed;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.highScore = 0;
        this.gamesPlayed = 0;
    }

    public User(String username, String password, int highScore, int gamesPlayed) {
        this.username = username;
        this.password = password;
        this.highScore = highScore;
        this.gamesPlayed = gamesPlayed;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getHighScore() { return highScore; }
    public int getGamesPlayed() { return gamesPlayed; }

    // Setters
    public void setHighScore(int highScore) { this.highScore = highScore; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    /**
     * Serialize user to a single line for file storage.
     * Format: username:password:highScore:gamesPlayed
     */
    public String toFileString() {
        return username + ":" + password + ":" + highScore + ":" + gamesPlayed;
    }

    /**
     * Deserialize a user from a file line.
     */
    public static User fromFileString(String line) {
        String[] parts = line.split(":");
        if (parts.length < 2) return null;
        String uname = parts[0];
        String pwd = parts[1];
        int hs = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        int gp = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
        return new User(uname, pwd, hs, gp);
    }

    @Override
    public String toString() {
        return "User{username='" + username + "', highScore=" + highScore + "}";
    }
}