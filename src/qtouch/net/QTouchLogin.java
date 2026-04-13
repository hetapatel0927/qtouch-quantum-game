package qtouch.net;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
/**
 * Login window for the QTouch system. Handles user sign-in, sign-out, and sign-up.
 */
public class QTouchLogin extends JFrame {
	/** Username text field */

    private JTextField userField;
    /** Password input field */

    private JPasswordField passField;
    /** Password input field */

    private JButton signInBtn;
    /** Sign-out button */

    private JButton signOutBtn;
    /** Sign-up button */

    private JButton signUpBtn;
    /**
     * Constructs the login window and initializes all UI components.
     */
    public QTouchLogin() {
        setTitle("Login System");
        setSize(420, 360);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel banner = new JLabel(new ImageIcon("img/qtouchSplash.jpg"));
        banner.setHorizontalAlignment(SwingConstants.CENTER);
        main.add(banner, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 2, 5, 5));
        center.setBorder(new EmptyBorder(10, 10, 10, 10));

        center.add(new JLabel("Username:"));
        userField = new JTextField();
        center.add(userField);

        center.add(new JLabel("Password:"));
        passField = new JPasswordField();
        center.add(passField);

        main.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        signInBtn = new JButton("Log In");
        signOutBtn = new JButton("Sign Out");
        signOutBtn.setEnabled(false);          
        row1.add(signInBtn);
        row1.add(signOutBtn);

        JPanel row2 = new JPanel(new BorderLayout());
        signUpBtn = new JButton("Sign Up");
        row2.add(signUpBtn, BorderLayout.CENTER);

        bottom.add(row1);
        bottom.add(row2);

        main.add(bottom, BorderLayout.SOUTH);
        add(main);

        signInBtn.addActionListener(e -> doLogin());
        signOutBtn.addActionListener(e -> doSignOut());
        signUpBtn.addActionListener(e -> doSignUp());
    }

    private void doLogin() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.");
            return;
        }

        boolean ok = QTouchDBConfig.loginUser(user, pass);

        if (ok) {
            signOutBtn.setEnabled(true);
            signInBtn.setEnabled(false);
            userField.setEditable(false);
            passField.setEditable(false);

            SwingUtilities.invokeLater(() -> {
                QTouchClient client = new QTouchClient();   
                client.setVisible(true);
            });

            dispose();

        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials.");
        }
    }

    private void doSignOut() {
        userField.setText("");
        passField.setText("");
        userField.setEditable(true);
        passField.setEditable(true);
        signInBtn.setEnabled(true);
        signOutBtn.setEnabled(false);
    }

    private void doSignUp() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.");
            return;
        }

        boolean created = QTouchDBConfig.registerUser(user, pass);

        if (created) {
            JOptionPane.showMessageDialog(this, "User created successfully.");
        } else {
            JOptionPane.showMessageDialog(this, "User already exists or DB error.");
        }
    }
    /**
     * Entry point for launching the login window.
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        QTouchDBConfig.initializeDatabase();

        SwingUtilities.invokeLater(() -> new QTouchLogin().setVisible(true));
    }
}
