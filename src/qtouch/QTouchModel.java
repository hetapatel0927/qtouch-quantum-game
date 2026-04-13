package qtouch;

/**
 * Model class – holds all constants, variables, and game states.
 * Defines configuration and data shared across the app.
 */
public class QTouchModel {
	/** Path to the game's image resources. */
    public static final String IMAGE_PATH = "C:\\CST8132 Homework\\A4F\\img\\";

    // ----------- GAME STATE VARIABLES -----------
    private int remainingCards = 24;
    private boolean isPlayer1Turn = false;
    private boolean isGameStarted = false;
    private boolean isPaused = false;
    private int timeLeft = 12;
    private String currentPosition = "0";
    private String savedGameConfig = "";
    /**
     * Stores the serialized saved game configuration.
     *
     * @param config the save string
     */
    public void setSavedGameConfig(String config) {
        this.savedGameConfig = config;
    }
    /**
     * Returns the serialized saved game configuration string.
     *
     * @return saved game config
     */
    public String getSavedGameConfig() {
        return savedGameConfig;
    }

    /** @return how many cards are left in the deck */

    public int getRemainingCards() { return remainingCards; }
    /**
     * Sets the number of cards remaining in the deck.
     *
     * @param remainingCards the amount left
     */
    public void setRemainingCards(int remainingCards) { this.remainingCards = remainingCards; }
    /** @return true if it's Player 1's turn */

    public boolean isPlayer1Turn() { return isPlayer1Turn; }
    /**
     * Sets the active player's turn.
     *
     * @param player1Turn true when Player 1 is active
     */
    public void setPlayer1Turn(boolean player1Turn) { isPlayer1Turn = player1Turn; }
    /** @return true if the game has started */
    public boolean isGameStarted() { return isGameStarted; }
    /**
     * Sets whether the game has started.
     *
     * @param gameStarted true to mark game as active
     */
    public void setGameStarted(boolean gameStarted) { isGameStarted = gameStarted; }
    /** @return true if the game is currently paused */

    public boolean isPaused() { return isPaused; }
    /**
     * Sets whether the game is paused.
     *
     * @param paused true to pause, false to resume
     */

    public void setPaused(boolean paused) { isPaused = paused; }
    /** Switches the active player turn. */

    public void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
    }
    /** @return current board position (0, 1, J, I, P, M) */

    public String getCurrentPosition() {
        return currentPosition;
    }
    /**
     * Sets the current board position.
     *
     * @param currentPosition the new board marker
     */
    public void setCurrentPosition(String currentPosition) {
        this.currentPosition = currentPosition;
    }
 

    /** @return time left in the player's turn */

    public int getTimeLeft() { return timeLeft; }
    /**
     * Sets the countdown timer for the turn.
     *
     * @param timeLeft seconds remaining
     */
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }
}
