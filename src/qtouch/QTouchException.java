package qtouch;
/**
 * Custom exception class for QTouch game errors.
 * Used to represent issues specific to QTouch operations.
 */
public class QTouchException extends Exception {
	/**
	 * Creates a new QTouchException with the specified message.
	 *
	 * @param message the detail message explaining the exception
	 */
    public QTouchException(String message) {
        super(message);
    }
}
