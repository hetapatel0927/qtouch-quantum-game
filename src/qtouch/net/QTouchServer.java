package qtouch.net;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 * QTouch game server handling connections, DB operations, and client threads.
 */
public class QTouchServer extends JFrame {
	/** The console area where server logs appear */
    private JTextArea console;
    /** The port input field */
    private JTextField portField;
    /** Server socket that listens for incoming client connections */

    private ServerSocket serverSocket;
    /** Flag indicating whether the server is currently running */

    private boolean isRunning = false;
    /** List of connected client handler threads */

    private final List<ClientHandler> clients = new ArrayList<>();
    /**
     * Creates the server UI and initializes all components.
     */
    public QTouchServer() {
        setTitle("QTouch Server");
        setSize(600, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel banner = new JLabel(new ImageIcon("img/QTouchServer.jpg"));
        banner.setHorizontalAlignment(SwingConstants.CENTER);
        main.add(banner, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel portPanel = new JPanel(new FlowLayout());
        portPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345", 10);
        portPanel.add(portField);
        center.add(portPanel);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 10));

        JButton startBtn = new JButton("Start");
        JButton createDBBtn = new JButton("Create DB");
        JButton showDBBtn = new JButton("Show DB");
        JButton finalizeBtn = new JButton("Finalize");
        JButton endBtn = new JButton("End");

        buttonPanel.add(startBtn);
        buttonPanel.add(createDBBtn);
        buttonPanel.add(showDBBtn);
        buttonPanel.add(finalizeBtn);
        buttonPanel.add(endBtn);

        center.add(buttonPanel);

        console = new JTextArea();
        console.setEditable(false);
        JScrollPane scroll = new JScrollPane(console);
        center.add(scroll);

        main.add(center, BorderLayout.CENTER);
        add(main);


        startBtn.addActionListener(this::startServer);
        createDBBtn.addActionListener(e -> {
            append("Creating database...");
            QTouchDBConfig.initializeDatabase();
            append("Database ready.");
        });
        showDBBtn.addActionListener(e -> showDatabase());
        finalizeBtn.addActionListener(e -> finalizeClients());
        endBtn.addActionListener(e -> stopServer());
    }
    /**
     * Appends a line of text to the server console window.
     * @param text the message to append
     */
    public void append(String text) {
        console.append(text + "\n");
    }

    private void startServer(ActionEvent e) {
        if (isRunning) {
            append("Server already running.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            append("Invalid port.");
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            append("Server started on port " + port);
            isRunning = true;

            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        append("Client connected.");

                        ClientHandler handler = new ClientHandler(clientSocket, this);
                        clients.add(handler);
                        handler.start();

                    } catch (IOException ex) {
                        if (isRunning) append("Server stopped listening.");
                    }
                }
            }).start();

        } catch (IOException ex) {
            append("ERROR: Port already in use.");
        }
    }

    private void showDatabase() {
        append("--- USERS TABLE ---");

        try (Connection conn = DriverManager.getConnection(QTouchDBConfig.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                append("id=" + rs.getInt("id") +
                        ", user=" + rs.getString("username"));
            }

        } catch (SQLException e) {
            append("DB Error: " + e.getMessage());
        }

        append("--- games TABLE ---");

        try (Connection conn = DriverManager.getConnection(QTouchDBConfig.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM games")) {

            while (rs.next()) {
                append("user=" + rs.getString("username") +
                        " | config=" + rs.getString("config")+
                " | saved_at=" + rs.getString("saved_at"));

            }

        } catch (SQLException e) {
            append("DB Error: " + e.getMessage());
        }

            }

    private void finalizeClients() {
        append("Closing all clients...");
        for (ClientHandler c : clients) {
            c.closeConnection();
        }
        clients.clear();
    }

    private void stopServer() {
        try {
            isRunning = false;

            finalizeClients();

            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();

            append("Server closed.");
            dispose();

        } catch (IOException ex) {
            append("Error closing server: " + ex.getMessage());
        }
    }

    /**
     * Entry point for launching the QTouch server application.
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QTouchServer().setVisible(true));
    }
}

class ClientHandler extends Thread {

    private Socket socket;
    private QTouchServer gui;
    private BufferedReader in;
    private PrintWriter out;
    private String lastgames = "";

    public ClientHandler(Socket socket, QTouchServer gui) {
        this.socket = socket;
        this.gui = gui;
    }

    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            int assignedId = this.hashCode();
            out.println(assignedId);
            out.flush();
            gui.append("Sent client ID: " + assignedId);
            String msg;

            while ((msg = in.readLine()) != null) {

                String[] parts = msg.split(QTouchProtocol.SEP);

                if (parts.length < 2) continue;
                String protocol = parts[1];

                gui.append("Received: " + msg);

                switch (protocol) {
                case QTouchProtocol.P1_SEND_GAME -> {
                    if (parts.length < 3) break;

                    lastgames = parts[2];
                    gui.append("Client sent circuit: " + lastgames);
                }

                case QTouchProtocol.P2_GET_GAME -> {
                    out.println(lastgames);
                }


                    case QTouchProtocol.P3_LOGIN -> {
                        if (parts.length < 4) {
                            out.println("FAIL");
                            continue;
                        }
                        boolean ok = QTouchDBConfig.loginUser(parts[2], parts[3]);
                        out.println(ok ? "OK" : "FAIL");
                    }

                    case QTouchProtocol.P4_SIGNUP -> {
                        if (parts.length < 4) {
                            out.println("Invalid signup");
                            continue;
                        }
                        boolean ok = QTouchDBConfig.registerUser(parts[2], parts[3]);
                        out.println(ok ? "User created." : "User exists!");
                    }

                    case QTouchProtocol.P5_SAVE -> {
                    	String user = parts[2];
                        String config = parts[3];
                        QTouchDBConfig.savegames(user, config);
                        out.println("OK_saved");
                        break;

                    }

                    case QTouchProtocol.P6_LOAD -> {
                    	String u = parts[2];
                        String loaded = QTouchDBConfig.loadgames(u);
                        out.println(loaded);
                        break;
                    }

                    case QTouchProtocol.P0_DISCONNECT -> {
                        gui.append("Client disconnected.");
                        closeConnection();
                        return;
                    }
                }
            }

        } catch (IOException e) {
            gui.append("Client disconnected.");
        }
    }
}
