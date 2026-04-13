package qtouch;

/**
 * Entry point for QTouch MVC.
 * Connects Model, View, and Controller.
 */
public class QTouchMVC {
	/**
	 * Entry point for launching the QTouch game.
	 * Initializes the model, splash screen, view, and controller.
	 *
	 * @param args command-line arguments (not used)
	 */
    public static void main(String[] args) {
        QTouchModel model = new QTouchModel();
        QTouchView view = new QTouchView(model);
        QTouchController controller = new QTouchController(model, view);

        // Splash screen
        QTouchSplash splash = new QTouchSplash(model);
        splash.showSplash();

        // Initialize main app
        controller.initialize();
    }
}
