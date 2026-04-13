package qtouch.net;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Handles all SQLite database operations for QTouch:
 * creating tables, user login/signup, and saving/loading game data.
 */
public class QTouchDBConfig {

    private static final Logger log = Logger.getLogger(QTouchDBConfig.class.getName());
    /** SQLite database connection URL */

    public static final String DB_URL = "jdbc:sqlite:qtouch.db";
    /**
     * Creates the required database tables (users and games) if they do not exist.
     */
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            log.info("Connected to SQLite");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS games (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    config TEXT NOT NULL,
                    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            log.info("Database ready.");

        } catch (SQLException e) {
            log.log(Level.SEVERE, "DB init error: {0}", e.getMessage());
        }
    }
    /**
     * Attempts to log in a user by checking the username and password.
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return true if the credentials match a user record, false otherwise
     */
    public static boolean loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            pst.setString(2, password);

            return pst.executeQuery().next();

        } catch (SQLException e) {
            return false;
        }
    }
    /**
     * Registers a new user in the database.
     * @param username the username to create
     * @param password the password for the account
     * @return true if creation succeeds, false if user exists or DB error
     */
    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            pst.setString(2, password);
            pst.executeUpdate();
            return true;

        } catch (SQLException e) {
            return false;
        }
    }
    /**
     * Saves a game configuration for a user.
     * @param username the user saving the game
     * @param gameConfig the serialized game configuration string
     */
    public static void savegames(String username, String gameConfig) {
        String sql = "INSERT INTO games(username, config) VALUES(?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            pst.setString(2, gameConfig);
            pst.executeUpdate();

        } catch (SQLException ignored) {
        }
    }
    /**
     * Loads the most recently saved game configuration for a user.
     * @param username the user whose game to load
     * @return the saved configuration string, or "NO_games" if none exist
     */
    public static String loadgames(String username) {
        String sql = "SELECT config FROM games WHERE username=? ORDER BY saved_at DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) return rs.getString("config");

        } catch (SQLException ignored) {
        }
        return "NO_games";
    }
}
