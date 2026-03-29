package auth;

import model.User;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

/**
 * AuthManager using MongoDB Atlas as global database.
 * All user data is stored in the cloud — accessible from any PC.
 *
 * Collection structure (MongoDB document):
 * {
 *   "username"    : "Pari",
 *   "password"    : "mypassword",
 *   "highScore"   : 1643,
 *   "gamesPlayed" : 5
 * }
 */
public class AuthManager {

    // -------------------------------------------------------
    // CHANGE THESE 3 VALUES to your own MongoDB Atlas details
    // -------------------------------------------------------
    private static final String CONNECTION_STRING =
        "mongodb+srv://memorygame_user:javaproject123@cluster0.z9l6qyv.mongodb.net/?appName=Cluster0";
    private static final String DATABASE_NAME   = "memorygame";
    private static final String COLLECTION_NAME = "users";
    // -------------------------------------------------------

    private MongoClient         mongoClient;
    private MongoCollection<Document> usersCollection;
    private boolean connected = false;

    public AuthManager() {
        connect();
    }

    /** Connect to MongoDB Atlas */
    private void connect() {
        try {
            mongoClient     = MongoClients.create(CONNECTION_STRING);
            MongoDatabase db = mongoClient.getDatabase(DATABASE_NAME);
            usersCollection  = db.getCollection(COLLECTION_NAME);
            connected        = true;
            System.out.println("[DB] Connected to MongoDB Atlas successfully.");
        } catch (Exception e) {
            connected = false;
            System.err.println("[DB] Failed to connect to MongoDB: " + e.getMessage());
            System.err.println("[DB] Falling back to local file storage.");
        }
    }

    // ----------------------------------------------------------
    //  Public API
    // ----------------------------------------------------------

    /**
     * Register a new user.
     * Returns true on success, false if username already taken.
     */
    public boolean register(String username, String password) {
        username = username.trim();
        if (username.isEmpty() || password.isEmpty()) return false;

        if (connected) {
            return registerMongo(username, password);
        } else {
            return registerLocal(username, password);
        }
    }

    /**
     * Validate login credentials.
     * Returns the User object on success, null on failure.
     */
    public User login(String username, String password) {
        username = username.trim();
        if (username.isEmpty() || password.isEmpty()) return null;

        if (connected) {
            return loginMongo(username, password);
        } else {
            return loginLocal(username, password);
        }
    }

    /**
     * Update user stats (high score, games played) in the database.
     */
    public void updateUser(User user) {
        if (connected) {
            updateMongo(user);
        } else {
            updateLocal(user);
        }
    }

    // ----------------------------------------------------------
    //  MongoDB Operations
    // ----------------------------------------------------------

    private boolean registerMongo(String username, String password) {
        try {
            // Check if username already exists
            Document existing = usersCollection.find(
                Filters.eq("username", username)).first();
            if (existing != null) return false;

            // Insert new user document
            Document newUser = new Document("username", username)
                    .append("password", password)
                    .append("highScore", 0)
                    .append("gamesPlayed", 0);
            usersCollection.insertOne(newUser);
            System.out.println("[DB] Registered user: " + username);
            return true;

        } catch (Exception e) {
            System.err.println("[DB] Register error: " + e.getMessage());
            return false;
        }
    }

    private User loginMongo(String username, String password) {
        try {
            // Find user with matching username AND password
            Document doc = usersCollection.find(
                Filters.and(
                    Filters.eq("username", username),
                    Filters.eq("password", password)
                )
            ).first();

            if (doc == null) return null;

            // Build User object from document
            return new User(
                doc.getString("username"),
                doc.getString("password"),
                doc.getInteger("highScore",   0),
                doc.getInteger("gamesPlayed", 0)
            );

        } catch (Exception e) {
            System.err.println("[DB] Login error: " + e.getMessage());
            return null;
        }
    }

    private void updateMongo(User user) {
        try {
            usersCollection.updateOne(
                Filters.eq("username", user.getUsername()),
                Updates.combine(
                    Updates.set("highScore",   user.getHighScore()),
                    Updates.set("gamesPlayed", user.getGamesPlayed())
                ),
                new UpdateOptions().upsert(false)
            );
            System.out.println("[DB] Updated user: " + user.getUsername());
        } catch (Exception e) {
            System.err.println("[DB] Update error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    //  Local File Fallback (when MongoDB is unavailable)
    //  Uses the original users.txt approach
    // ----------------------------------------------------------

    private static final String DATA_DIR  = "data";
    private static final String USER_FILE = DATA_DIR + java.io.File.separator + "users.txt";

    private boolean registerLocal(String username, String password) {
        java.util.Map<String, User> users = loadLocalUsers();
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, password));
        saveLocalUsers(users);
        return true;
    }

    private User loginLocal(String username, String password) {
        java.util.Map<String, User> users = loadLocalUsers();
        User user = users.get(username);
        if (user == null || !user.getPassword().equals(password)) return null;
        return user;
    }

    private void updateLocal(User user) {
        java.util.Map<String, User> users = loadLocalUsers();
        users.put(user.getUsername(), user);
        saveLocalUsers(users);
    }

    private java.util.Map<String, User> loadLocalUsers() {
        java.util.Map<String, User> users = new java.util.HashMap<String, User>();
        java.io.File file = new java.io.File(USER_FILE);
        if (!file.exists()) return users;
        try (java.io.BufferedReader reader =
                 new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                User u = User.fromFileString(line.trim());
                if (u != null) users.put(u.getUsername(), u);
            }
        } catch (java.io.IOException e) {
            System.err.println("[Local] Error loading users: " + e.getMessage());
        }
        return users;
    }

    private void saveLocalUsers(java.util.Map<String, User> users) {
        new java.io.File(DATA_DIR).mkdirs();
        try (java.io.PrintWriter writer =
                 new java.io.PrintWriter(new java.io.FileWriter(USER_FILE))) {
            for (User u : users.values()) writer.println(u.toFileString());
        } catch (java.io.IOException e) {
            System.err.println("[Local] Error saving users: " + e.getMessage());
        }
    }

    /** Close MongoDB connection when app exits */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("[DB] MongoDB connection closed.");
        }
    }
}