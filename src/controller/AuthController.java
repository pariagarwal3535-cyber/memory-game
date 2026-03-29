package controller;

import auth.AuthManager;
import model.User;

/**
 * Bridges the authentication UI with the AuthManager.
 */
public class AuthController {

    private AuthManager authManager;
    private User currentUser;

    public AuthController() {
        this.authManager = new AuthManager();
    }

    /**
     * Attempt login. Returns true and stores currentUser on success.
     */
    public boolean login(String username, String password) {
        if (username == null || username.isBlank()) return false;
        if (password == null || password.isEmpty())  return false;
        User user = authManager.login(username, password);
        if (user != null) {
            currentUser = user;
            return true;
        }
        return false;
    }

    /**
     * Attempt registration. Returns true on success.
     */
    public boolean register(String username, String password, String confirmPassword) {
        if (username == null || username.isBlank()) return false;
        if (password == null || password.isEmpty())  return false;
        if (!password.equals(confirmPassword))       return false;
        if (password.length() < 4)                   return false;
        return authManager.register(username, password);
    }

    /** Update current user stats */
    public void updateCurrentUser(int newHighScore, int gamesPlayed) {
        if (currentUser == null) return;
        if (newHighScore > currentUser.getHighScore())
            currentUser.setHighScore(newHighScore);
        currentUser.setGamesPlayed(gamesPlayed);
        authManager.updateUser(currentUser);
    }

    public User getCurrentUser() { return currentUser; }
    public void logout() { currentUser = null; }

    // --- Validation helpers (used by view for inline feedback) ---
    public String validateUsername(String username) {
        if (username == null || username.isBlank()) return "Username cannot be empty.";
        if (username.contains(":"))                 return "Username cannot contain ':'";
        if (username.length() < 3)                  return "Username must be at least 3 characters.";
        return null; // valid
    }

    public String validatePassword(String password) {
        if (password == null || password.isEmpty()) return "Password cannot be empty.";
        if (password.length() < 4)                  return "Password must be at least 4 characters.";
        return null; // valid
    }
}