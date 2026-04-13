package qtouch.net;
/**
 * Represents a QTouch user with id, username, and password.
 */
public class QTouchUser {
    private int id;
    private String username;
    private String password;
    /**
     * Creates a user with a known ID (typically loaded from database).
     * @param id the user ID
     * @param username the username
     * @param password the password
     */
    public QTouchUser(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }
    /**
     * Creates a user with a known ID (typically loaded from database).
     * @param username the username
     * @param password the password
     */
    public QTouchUser(String username, String password) {
        this(-1, username, password);
    }
    /**
     * Gets the user's database ID.
     * @return the ID
     */
    public int getId() { return id; }
    /**
     * Gets the username.
     * @return the username
     */
    public String getUsername() { return username; }
    /**
     * Gets the user's password.
     * @return the password
     */
    public String getPassword() { return password; }
}
