package qtouch.net;
/**
 * Simple model class that stores the current QTouch game configuration.
 */
public class QTouchGameModel {
    private String game;
    /**
     * Creates a new game model with the given configuration.
     * @param game the serialized game configuration string
     */
    public QTouchGameModel(String game) {
        this.game = game;
    }
    /**
     * Returns the current game configuration.
     * @return the game configuration string
     */
    public String getGame() {
        return game;
    }
    /**
     * Updates the stored game configuration.
     * @param game the new game configuration string
     */
    public void setGame(String game) {
        this.game = game;
    }
}
