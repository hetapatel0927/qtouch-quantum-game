package qtouch.net;

import qtouch.QTouchView;
import qtouch.QTouchModel;
import qtouch.QTouchController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
/**
 * QTouchClient handles the GUI and client-side networking
 * for the QTouch game application.
 */
public class QTouchClient extends JFrame {
	/** Input fields for username, server IP, and port number. */

    private JTextField ipField, portField, userField;
    /** Console area used to display client messages and logs. */
    private JTextArea consoleArea;
    /** Socket used for client-server communication. */

    private Socket socket;
    /** Reader for incoming server messages. */

    private BufferedReader in;
    /** Writer for sending messages to the server. */

    private PrintWriter out;
    /** Unique client ID assigned by the server. */

    private String clientId = "0";
    /** Button to initiate connection to the server. */

    private JButton connectBtn;
    /** Button to disconnect from the server. */

    private JButton endBtn;
    /** Button to create a new local game configuration. */

    private JButton newGameBtn;
    /** Button to send the current game state to the server. */

    private JButton sendGameBtn;
    /** Button to receive a saved game state from the server. */

    private JButton receiveGameBtn;
    /** Button to launch the MVC game using the current game configuration. */

    private JButton runGameBtn;
    /** Indicates whether the client is currently connected to the server. */

    private boolean connected = false;

    /** Stores the current game configuration or full saved game state. */
    private String currentGame = "";
    /**
     * Creates a new QTouchClient window and initializes
     * all UI components and network setup.
     */
    public QTouchClient() {
        setTitle("QTouch Client");
        setSize(700, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        JLabel banner = new JLabel(new ImageIcon("img/QTouchClient.jpg"));
        banner.setHorizontalAlignment(SwingConstants.CENTER);
        add(banner, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

        top.add(new JLabel("User:"));
        userField = new JTextField("", 10);
        top.add(userField);

        top.add(new JLabel("Server:"));
        ipField = new JTextField("localhost", 12);
        top.add(ipField);

        top.add(new JLabel("Port:"));
        portField = new JTextField("12345", 6);
        top.add(portField);

        connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> attemptConnect());
        top.add(connectBtn);

        endBtn = new JButton("End");
        endBtn.addActionListener(e -> disconnect());
        top.add(endBtn);

        center.add(top);

        // CLIENT BUTTONS (NO SAVE / NO LOAD)
        JPanel mid = new JPanel(new FlowLayout(FlowLayout.LEFT));

        newGameBtn = new JButton("New Game");
        sendGameBtn = new JButton("Send Game");
        receiveGameBtn = new JButton("Receive Game");
        runGameBtn = new JButton("Run Game");

        newGameBtn.setEnabled(false);
        sendGameBtn.setEnabled(false);
        receiveGameBtn.setEnabled(false);
        runGameBtn.setEnabled(false);

        mid.add(newGameBtn);
        mid.add(sendGameBtn);
        mid.add(receiveGameBtn);
        mid.add(runGameBtn);

        center.add(mid);
        add(center, BorderLayout.CENTER);

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setPreferredSize(new Dimension(700, 200));
        add(scroll, BorderLayout.SOUTH);

        newGameBtn.addActionListener(e -> {
            currentGame = "H,X,Z,H,T";
            consoleArea.append("New game configuration created: " + currentGame + "\n");
        });

        sendGameBtn.addActionListener(e -> {
            if (!connected) {
                consoleArea.append("Not connected.\n");
                return;
            }
            if (currentGame.isEmpty()) {
                consoleArea.append("No game to send.\n");
                return;
            }

            out.println(clientId + "#P1#" + currentGame);
            consoleArea.append("Game sent to server.\n");
        });

       
        receiveGameBtn.addActionListener(e -> {
            if (!connected) {
                consoleArea.append("Not connected.\n");
                return;
            }

            out.println(clientId + "#P2");
            try {
                String received = in.readLine();
                currentGame = received;
                consoleArea.append("Received game: " + received + "\n");
            } catch (IOException ex) {
                consoleArea.append("Receive failed.\n");
            }
        });

        runGameBtn.addActionListener(e -> {

           
            if (!currentGame.isEmpty()) {
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "Resume saved game?\nYES = Continue saved game\nNO = Start new game",
                        "Resume Game",
                        JOptionPane.YES_NO_OPTION
                );

                if (choice == JOptionPane.NO_OPTION) {
                    // Create NEW game
                    currentGame = "H,X,Z,H,T";  // default new config
                    consoleArea.append("New game created: " + currentGame + "\n");
                }
            } else {
                // No saved game → always make new
                currentGame = "H,X,Z,H,T";
                consoleArea.append("New game created: " + currentGame + "\n");
            }

            // Now run MVC with whatever currentGame is
            consoleArea.append("Launching MVC Game...\n");

            QTouchModel model = new QTouchModel();
            model.setSavedGameConfig(currentGame);

            QTouchView view = new QTouchView(model);
            QTouchController controller = new QTouchController(model, view);

            // Save callback updates client copy of state
            controller.setSaveCallback(fullState -> {
                currentGame = fullState;
                consoleArea.append("Game state updated.\n");
            });

            controller.initialize();
        });

    }

    private void flushInput() {
        try {
            while (in.ready()) {
                in.readLine();
            }
        } catch (IOException ignored) {}
    }

    private void disconnect() {
        if (!connected) return;

        out.println(clientId + "#P0");
        try { socket.close(); } catch (IOException ignored) {}

        consoleArea.append("Disconnected.\n");

        connected = false;
        connectBtn.setEnabled(true);

        newGameBtn.setEnabled(false);
        sendGameBtn.setEnabled(false);
        receiveGameBtn.setEnabled(false);
        runGameBtn.setEnabled(false);
    }

    private void attemptConnect() {
        String ip = ipField.getText().trim();
        int port;

        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            consoleArea.append("Invalid port.\n");
            return;
        }

        try {
            consoleArea.append("Attempting connection...\n");

            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String id = in.readLine();
            if (id == null) {
                consoleArea.append("Server did not send client ID.\n");
                return;
            }
            clientId = id;

            connected = true;
            consoleArea.append("Connected. Client ID = " + clientId + "\n");

            newGameBtn.setEnabled(true);
            sendGameBtn.setEnabled(true);
            receiveGameBtn.setEnabled(true);
            runGameBtn.setEnabled(true);

            connectBtn.setEnabled(false);

        } catch (IOException ex) {
            consoleArea.append("Connection failed.\n");
        }
    }
    /**
     * Entry point of the QTouch client application.
     * Launches the QTouchClient GUI.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QTouchClient().setVisible(true));
    }
}
