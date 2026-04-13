package qtouch;

import javax.swing.*;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;

import java.awt.event.*;
import java.util.*;
import java.util.function.Consumer;   // 
/**
 * Controller class for the QTouch game. 
 * It manages game logic, interactions between the model and view,
 * and user actions during gameplay.
 */
public class QTouchController {
    private final QTouchModel model;
    private final QTouchView view;
    private javax.swing.Timer turnTimer;
    private final Random rng = new Random();
    private int player1Score = 0;
    private int player2Score = 0;
    private static final int WIN_SCORE = 3;
    private int touchdownTarget = 3; 

    private final String basePath = "C:\\CST8132 Homework\\A4F\\img\\";
    private final List<String> deck = new ArrayList<>(Arrays.asList(
            "b.jpg", "z.jpg", "s.jpg", "y.jpg", "m.jpg", "x.jpg", "h.jpg"));
    private final Set<String> usedCards = new HashSet<>();

    //  NEW: callback to send full save string back to client
    private Consumer<String> saveCallback;
    /**
     * Sets a callback that receives the save string whenever
     * a save operation occurs.
     *
     * @param cb a Consumer that accepts the save string
     */
    public void setSaveCallback(Consumer<String> cb) {
        this.saveCallback = cb;
    }
    /**
     * Creates a QTouchController with the given model and view.
     *
     * @param model the QTouch game model
     * @param view  the QTouch game view
     */
    public QTouchController(QTouchModel model, QTouchView view) {
        this.model = model;
        this.view = view;
    }
    /**
     * Initializes the controller, sets listeners, restores saved games,
     * loads resources, and prepares the game to start.
     */
    public void initialize() {
    	
    	
    	//  Check if MVC received a saved game config
    	if (model.getSavedGameConfig() != null && !model.getSavedGameConfig().isEmpty()) {
    	    System.out.println("Loaded saved game config for MVC = " + model.getSavedGameConfig());
            String cfg = model.getSavedGameConfig();
            // If it's a full save string (with '|') restore EVERYTHING
            if (cfg.contains("|")) {
                restoreSavedGame(cfg);
            }
    	}

        view.exitItem.addActionListener(e -> view.dispose());
        view.startPauseButton.addActionListener(e -> toggleGame());

        // NEW: Save Game menu item (you will add this in QTouchView)
        if (view.saveGameItem != null) {
            view.saveGameItem.addActionListener(e -> {
                String save = buildSaveString();
                model.setSavedGameConfig(save);
                if (saveCallback != null) {
                    saveCallback.accept(save);  // send back to client
                }
                JOptionPane.showMessageDialog(view, "Game saved!");
            });
        }
        
        view.newGameItem.addActionListener(e -> {
            if (model.isGameStarted()) {
                int choice = JOptionPane.showConfirmDialog(
                        view,
                        "A game is currently in progress.\nDo you want to quit and start a new game?",
                        "Confirm New Game",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (choice != JOptionPane.YES_OPTION) return;
            }

            // Reset everything for new game
            player1Score = 0;
            player2Score = 0;
            view.player1Score.setText("Score: 0");
            view.player2Score.setText("Score: 0");
            usedCards.clear();
            model.setRemainingCards(24);
            model.setGameStarted(false);
            model.setPaused(false);
            model.setCurrentPosition("0");

            view.drawCardLabel1.setText("Draw Card (24)");
            assignUniqueRandomCards(view.leftPanel);
            assignUniqueRandomCards(view.rightPanel);
            view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + "board.jpg", 900, 600));

            startGame(); // ask touchdown + restart
        });
     // SAVE GAME MENU ACTION
        view.saveGameItem.addActionListener(e -> {
            String save = buildSaveString();
            if (saveCallback != null) {
                saveCallback.accept(save);  // send to CLIENT
            }
            JOptionPane.showMessageDialog(view, "Game saved!");
        });

        view.rollButton.addActionListener(e -> rollDice());

        setupNameEditing(view.player1Title, true);
        setupNameEditing(view.player2Title, false);
        setupImageChange(view.leftPanel, true);
        setupImageChange(view.rightPanel, false);

        assignUniqueRandomCards(view.leftPanel);
        assignUniqueRandomCards(view.rightPanel);

        attachCardListeners(view.leftPanel, true);
        attachCardListeners(view.rightPanel, false);

        view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + "board.jpg", 900, 600));
        view.setVisible(true);
    }

    private void setupNameEditing(JLabel label, boolean left) {
        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!model.isGameStarted()) {
                    String newName = JOptionPane.showInputDialog(view,
                            "Enter new name for " + (left ? "Player 1" : "Player 2"),
                            label.getText());
                    if (newName != null && !newName.trim().isEmpty())
                        label.setText(newName.trim());
                } else msg("Cannot change name after game start!");
            }
        });
    }

    private void setupImageChange(Container sidePanel, boolean left) {
        for (Component comp : getAllComponents(sidePanel)) {
            if (comp instanceof JLabel lbl) {
                Icon icon = lbl.getIcon();
                if (icon instanceof ImageIcon img && img.getIconWidth() >= 190 && img.getIconHeight() >= 240) {
                    lbl.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (!model.isGameStarted()) {
                                JFileChooser fc = new JFileChooser();
                                fc.setDialogTitle("Select new image for " + (left ? "Player 1" : "Player 2"));
                                if (fc.showOpenDialog(view) == JFileChooser.APPROVE_OPTION) {
                                    ImageIcon original = new ImageIcon(fc.getSelectedFile().getAbsolutePath());
                                    Image scaledImg = original.getImage()
                                            .getScaledInstance(200, 250, Image.SCALE_SMOOTH);
                                    ImageIcon scaledIcon = new ImageIcon(scaledImg);
                                    scaledIcon.setDescription(fc.getSelectedFile().getName());
                                    lbl.setIcon(scaledIcon);
                                }
                            } else msg("Cannot change image after game start!");
                        }
                    });
                    break;
                }
            }
        }
    }

    private JLabel findDiscardLabel1() {
        for (Component c : view.getContentPane().getComponents()) {
            if (c instanceof JPanel p) {
                for (Component sub : getAllComponents(p)) {
                    if (sub instanceof JLabel lbl) {
                        Icon ic = lbl.getIcon();
                        if (ic instanceof ImageIcon img &&
                                img.getIconWidth() >= 95 && img.getIconWidth() <= 110 &&
                                lbl.getText() == null) {
                            Container parent = lbl.getParent();
                            for (Component sib : parent.getComponents()) {
                                if (sib instanceof JLabel textLbl &&
                                        "Discard Pile".equals(textLbl.getText())) {
                                    return lbl;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void assignUniqueRandomCards(Container panel) {
        List<JLabel> cardLabels = getCardLabels(panel);
        Collections.shuffle(deck);
        for (JLabel card : cardLabels) {
            String nextCard = drawUniqueCard();

            // create scaled icon WITH description
            ImageIcon original = new ImageIcon(model.IMAGE_PATH + nextCard);
            Image scaledImg = original.getImage()
                    .getScaledInstance(100, 150, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImg);
            scaledIcon.setDescription(nextCard); // 🔥 key line
            card.setIcon(scaledIcon);
        }
    }

    private String drawUniqueCard() {
        for (String c : deck) {
            if (!usedCards.contains(c)) {
                usedCards.add(c);
                return c;
            }
        }
        usedCards.clear();
        return deck.get(rng.nextInt(deck.size()));
    }

    private void attachCardListeners(Container panel, boolean leftSide) {
        for (JLabel card : getCardLabels(panel)) {
            card.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (!model.isGameStarted() || model.isPaused()) {
                        msg("Start the game first!");
                        return;
                    }
                    if ((leftSide && !model.isPlayer1Turn()) || (!leftSide && model.isPlayer1Turn()))
                        return;

                    JLabel discardImg = findDiscardLabel1();
                    if (discardImg != null) discardImg.setIcon(card.getIcon());

                    //  SAFE CARD NAME EXTRACTION (only from description)
                    String cardName = null;
                    Icon icon = card.getIcon();
                    if (icon instanceof ImageIcon img) {
                        cardName = img.getDescription();
                    }
                    if (cardName == null || cardName.isEmpty()) {
                        msg("DEBUG: Card description missing for " + icon);
                        return; // no move; prevents ImageIcon@... bug
                    }
                    cardName = cardName.toLowerCase();

                    usedCards.add(cardName);

                    //  give new card with proper description
                    String newCard = drawUniqueCard();
                    ImageIcon original = new ImageIcon(model.IMAGE_PATH + newCard);
                    Image scaledImg = original.getImage()
                            .getScaledInstance(100, 150, Image.SCALE_SMOOTH);
                    ImageIcon scaledIcon = new ImageIcon(scaledImg);
                    scaledIcon.setDescription(newCard);
                    card.setIcon(scaledIcon);

                    if (model.getRemainingCards() > 0) {
                        model.setRemainingCards(model.getRemainingCards() - 1);
                        view.drawCardLabel1.setText("Draw Card (" + model.getRemainingCards() + ")");
                    }
                    if (model.getRemainingCards() == 0) {
                        usedCards.clear(); //  reset deck
                        model.setRemainingCards(24);
                        msg("Deck reshuffled!");
                    }


                    updateBoardForCard(model.getCurrentPosition(), cardName);
                    switchTurn();
                }
            });
        }
    }

    private java.util.List<JLabel> getCardLabels(Container panel) {
        java.util.List<JLabel> cards = new ArrayList<>();
        for (Component comp : getAllComponents(panel)) {
            if (comp instanceof JLabel lbl && lbl.getIcon() instanceof ImageIcon img) {
                if (img.getIconWidth() >= 95 && img.getIconWidth() <= 110) cards.add(lbl);
            }
        }
        return cards;
    }

    private Component[] getAllComponents(Container c) {
        java.util.List<Component> list = new ArrayList<>();
        for (Component comp : c.getComponents()) {
            list.add(comp);
            if (comp instanceof Container)
                Collections.addAll(list, getAllComponents((Container) comp));
        }
        return list.toArray(new Component[0]);
    }

    private void toggleGame() {
        if (!model.isGameStarted()) {
            model.setGameStarted(true);
            model.setPaused(false);
            view.startPauseButton.setIcon(view.pauseIcon);
            startGame();
        } else if (!model.isPaused()) {
            model.setPaused(true);
            view.startPauseButton.setIcon(view.startIcon);
            if (turnTimer != null) turnTimer.stop();
            msg("Game paused!");
        } else {
            model.setPaused(false);
            view.startPauseButton.setIcon(view.pauseIcon);
            startTimer();
        }
    }

    private void startGame() {
        // Ask for touchdown target (mandatory input with default 3)
        int inputVal = 0;
        while (inputVal <= 0) {
            String input = JOptionPane.showInputDialog(
                    view,
                    "Enter total touchdowns needed to win (e.g., 1, 2, 3).\n" +
                    "Click Cancel or leave blank for default: 3",
                    "3"
            );

            if (input == null || input.trim().isEmpty()) {
                touchdownTarget = 3;
                break; // user cancelled or blank, go default
            }

            try {
                inputVal = Integer.parseInt(input.trim());
                if (inputVal > 0) touchdownTarget = inputVal;
                else msg("Please enter a number greater than 0.");
            } catch (NumberFormatException ex) {
                msg("Invalid input. Enter a number like 1, 2, or 3.");
            }
        }

        boolean startWithP1 = rng.nextBoolean();
        model.setCurrentPosition("0");
        model.setPlayer1Turn(startWithP1);
        view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + "board.jpg", 900, 600));
        updateTurnBorders();
        startTimer();

        msg("Game started! First to " + touchdownTarget + " touchdowns wins.\nRoll the dice!");
        countdownAndStart(); 

    }
    private void countdownAndStart() {
        model.setPaused(true);
        if (turnTimer != null) turnTimer.stop();

        // create a non-blocking popup window
        JDialog countdownDialog = new JDialog(view, "Get Ready!", false);
        countdownDialog.setSize(300, 200);
        countdownDialog.setLocationRelativeTo(view);
        countdownDialog.setUndecorated(true);
        countdownDialog.getContentPane().setBackground(Color.WHITE);

        JLabel countLabel = new JLabel("3", SwingConstants.CENTER);
        countLabel.setFont(new Font("Arial", Font.BOLD, 72));
        countLabel.setForeground(Color.BLUE);
        countdownDialog.add(countLabel, BorderLayout.CENTER);

        countdownDialog.setVisible(true); // non-blocking now 

        final int[] count = {3};
        javax.swing.Timer countdownTimer = new javax.swing.Timer(1000, e -> {
            count[0]--;
            if (count[0] > 0) {
                countLabel.setText(String.valueOf(count[0]));
            } else {
                ((javax.swing.Timer) e.getSource()).stop();
                countdownDialog.dispose();

                model.setPaused(false);
                view.startPauseButton.setIcon(view.pauseIcon);
                startTimer();
                msg("Go! The game has started!");
            }
        });

        countdownTimer.start();
    }


    private void startTimer() {
        if (turnTimer != null) turnTimer.stop();
        model.setTimeLeft(12);
        view.timeLabel.setText("Time left: " + model.getTimeLeft() + "s");

        turnTimer = new javax.swing.Timer(1000, e -> {
            if (!model.isPaused()) {
                model.setTimeLeft(model.getTimeLeft() - 1);
                view.timeLabel.setText("Time left: " + model.getTimeLeft() + "s");
                if (model.getTimeLeft() <= 0) switchTurn();
            }
        });
        turnTimer.start();
    }

    private void switchTurn() {
        model.setPlayer1Turn(!model.isPlayer1Turn());
        updateTurnBorders();
        startTimer();
    }

    private void updateTurnBorders() {
        view.leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(model.isPlayer1Turn() ? Color.RED : Color.BLACK, 3), "Player 1"));
        view.rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(!model.isPlayer1Turn() ? Color.RED : Color.BLACK, 3), "Player 2"));
        view.currentPlayerLabel.setText("Current Player: " +
                (model.isPlayer1Turn() ? view.player1Title.getText() : view.player2Title.getText()));
    }

    private void rollDice() {
        if (!model.isGameStarted() || model.isPaused()) {
            msg("Start the game first!");
            return;
        }

        int dice = rng.nextInt(2);
        view.diceLabel.setText(String.valueOf(dice));

        if (dice == 0) {
            view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + "qtboard0.jpg", 900, 600));
            model.setCurrentPosition("0");  // stays at 0
        } else {
            view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + "qtboard1.jpg", 900, 600));
            model.setCurrentPosition("1");  // move to 1 when dice = 1
        }

        System.out.println("DEBUG → Dice rolled: " + dice + ", Position: " + model.getCurrentPosition());
    }

    private void updateBoardForCard(String currentPos, String selectedCard) {
        if (selectedCard == null) {
            msg(" No card detected.");
            return;
        }

        String pos = (currentPos == null) ? "0" : currentPos.trim().toUpperCase();
        String card = selectedCard.trim().toLowerCase();
        String nextBoard = null;

        System.out.println("DEBUG → pos=" + pos + " card=" + card); //  track logic

        switch (pos) {
            case "0" -> {
                if (card.equals("h.jpg")) nextBoard = "qtboardP.jpg";
                else if (card.equals("b.jpg")) nextBoard = "qtboardJ.jpg";
                else if (card.equals("x.jpg") || card.equals("y.jpg")) nextBoard = "qtboard1.jpg";
            }
            case "J" -> {
                if (card.equals("b.jpg")) nextBoard = "qtboard1.jpg";
                else if (card.equals("x.jpg") || card.equals("z.jpg") || card.equals("h.jpg")) nextBoard = "qtboardI.jpg";
                if (card.equals("s.jpg")) nextBoard = "qtboardP.jpg";
            }
            case "1" -> {
                if (card.equals("h.jpg")) nextBoard = "qtboardM.jpg";
                else if (card.equals("b.jpg")) nextBoard = "qtboardI.jpg";
                else if (card.equals("x.jpg") || card.equals("y.jpg")) nextBoard = "qtboard0.jpg";
            }
            case "I" -> {
                if (card.equals("x.jpg") || card.equals("z.jpg") || card.equals("h.jpg")) nextBoard = "qtboardJ.jpg";
                else if (card.equals("s.jpg")) nextBoard = "qtboardM.jpg";
            }
            default -> msg(" Unknown position: " + pos);
        }

        if (nextBoard != null) {
            view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + nextBoard, 900, 600));
            model.setCurrentPosition(nextBoard.replace("qtboard", "").replace(".jpg", "").toUpperCase());
            System.out.println("DEBUG → new position = " + model.getCurrentPosition());
        } else {
            msg(" Invalid move from " + pos + " with card " + card);
        }
     // Update board image and position
        if (nextBoard != null) {
            view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + nextBoard, 900, 600));
            String newPos = nextBoard.replace("qtboard", "").replace(".jpg", "").toUpperCase();
            model.setCurrentPosition(newPos);

            if (newPos.equals("P") || newPos.equals("M")) {
                if (model.isPlayer1Turn()) {
                    player1Score++;
                    view.player1Score.setText("Score: " + player1Score);
                    msg(" " + view.player1Title.getText() + " SCORED A TOUCHDOWN!");
                } else {
                    player2Score++;
                    view.player2Score.setText("Score: " + player2Score);
                    msg("" + view.player2Title.getText() + " SCORED A TOUCHDOWN!");
                }

             //  Victory check
                if (player1Score >= touchdownTarget || player2Score >= touchdownTarget) {
                    String winner = player1Score >= touchdownTarget
                            ? view.player1Title.getText()
                            : view.player2Title.getText();

                    JLabel victoryLabel = new JLabel(" " + winner.toUpperCase() + " WINS THE GAME! ", SwingConstants.CENTER);
                    victoryLabel.setFont(new Font("Ariel", Font.BOLD, 36));
                    victoryLabel.setForeground(Color.RED);

                    JOptionPane.showMessageDialog(
                            view,
                            victoryLabel,
                            "VICTORY!",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    int choice = JOptionPane.showConfirmDialog(
                            view,
                            "Would you like to start a new game?",
                            "Play Again?",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        player1Score = 0;
                        player2Score = 0;
                        view.player1Score.setText("Score: 0");
                        view.player2Score.setText("Score: 0");
                        usedCards.clear();
                        model.setRemainingCards(24);
                        model.setGameStarted(false);
                        model.setPaused(false);
                        model.setCurrentPosition("0");

                        view.drawCardLabel1.setText("Draw Card (24)");

                        assignUniqueRandomCards(view.leftPanel);
                        assignUniqueRandomCards(view.rightPanel);

                        view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + "board.jpg", 900, 600));

                        startGame();
                    } else {
                        view.dispose();
                    }
                    return;
                }
            }
        }
    }
  
    

    /**
     * Builds and returns a complete save string representing the
     * entire current game state, including positions, cards, scores,
     * timers, portraits, and used cards.
     *
     * @return the full serialized game state as a String
     */
    public String buildSaveString() {
        String pos = (model.getCurrentPosition() == null ? "0" : model.getCurrentPosition());
        String turn = model.isPlayer1Turn() ? "P1" : "P2";
        int rem = model.getRemainingCards();
        int time = model.getTimeLeft();
        String diceVal = view.diceLabel.getText().trim();
        if (diceVal.isEmpty()) diceVal = "0";

        String p1Name = view.player1Title.getText().replace("|", "");
        String p2Name = view.player2Title.getText().replace("|", "");

        // portraits
        String p1Img = getPortraitFileName(view.leftPanel, "RF.jpg");
        String p2Img = getPortraitFileName(view.rightPanel, "R.jpg");

        // cards
        String leftCards = String.join(",", getCardFileNames(view.leftPanel));
        String rightCards = String.join(",", getCardFileNames(view.rightPanel));

        // discard
        String discard = "NONE";
        JLabel d = findDiscardLabel1();
        if (d != null && d.getIcon() instanceof ImageIcon di) {
            String desc = di.getDescription();
            if (desc != null && !desc.isBlank()) discard = desc;
        }

        // used
        String used = String.join(",", usedCards);

        // scores
        int t1 = player1Score;
        int t2 = player2Score;

        // pos,turn,rem,time,target,dice|p1Score,p2Score|p1Name,p2Name|p1Img,p2Img|L:...|R:...|D:...|U:...
        StringBuilder sb = new StringBuilder();
        sb.append(pos).append(",").append(turn).append(",")
          .append(rem).append(",").append(time).append(",")
          .append(touchdownTarget).append(",").append(diceVal).append("|");

        sb.append(t1).append(",").append(t2).append("|");
        sb.append(p1Name).append(",").append(p2Name).append("|");
        sb.append(p1Img).append(",").append(p2Img).append("|");

        sb.append("L:").append(leftCards).append("|");
        sb.append("R:").append(rightCards).append("|");
        sb.append("D:").append(discard).append("|");
        sb.append("U:").append(used);

        return sb.toString();
    }

    private String getPortraitFileName(Container panel, String defaultFile) {
        for (Component c : getAllComponents(panel)) {
            if (c instanceof JLabel lbl && lbl.getIcon() instanceof ImageIcon img) {
                if (img.getIconWidth() >= 190 && img.getIconHeight() >= 240) {
                    String desc = img.getDescription();
                    return (desc == null || desc.isBlank()) ? defaultFile : desc;
                }
            }
        }
        return defaultFile;
    }

    private java.util.List<String> getCardFileNames(Container panel) {
        java.util.List<String> list = new ArrayList<>();
        for (JLabel lbl : getCardLabels(panel)) {
            if (lbl.getIcon() instanceof ImageIcon img) {
                String desc = img.getDescription();
                if (desc != null && !desc.isBlank()) list.add(desc);
            }
        }
        return list;
    }

    private void restoreSavedGame(String cfg) {
        try {
            String[] parts = cfg.split("\\|");
            if (parts.length < 8) {
                System.out.println("SAVE STRING TOO SHORT, STARTING NORMAL GAME");
                return;
            }

            // part 0: pos,turn,rem,time,target,dice
            String[] a0 = parts[0].split(",");
            String pos = a0[0];
            String turn = a0[1];
            int rem = Integer.parseInt(a0[2]);
            int time = Integer.parseInt(a0[3]);
            touchdownTarget = Integer.parseInt(a0[4]);
            String diceVal = a0[5];

            // part 1: scores
            String[] a1 = parts[1].split(",");
            player1Score = Integer.parseInt(a1[0]);
            player2Score = Integer.parseInt(a1[1]);

            // part 2: names
            String[] a2 = parts[2].split(",", 2);
            String p1Name = a2[0];
            String p2Name = a2.length > 1 ? a2[1] : "";

            // part 3: portraits
            String[] a3 = parts[3].split(",", 2);
            String p1Img = a3[0];
            String p2Img = a3.length > 1 ? a3[1] : "R.jpg";

            // part 4: left cards
            String leftCardsRaw = parts[4].substring(2); // skip "L:"
            String[] leftCards = leftCardsRaw.isBlank() ? new String[0] : leftCardsRaw.split(",");

            // part 5: right cards
            String rightCardsRaw = parts[5].substring(2); // skip "R:"
            String[] rightCards = rightCardsRaw.isBlank() ? new String[0] : rightCardsRaw.split(",");

            // part 6: discard
            String discardRaw = parts[6].substring(2); // skip "D:"
            String discardFile = discardRaw.equals("NONE") ? null : discardRaw;

            // part 7: used
            String usedRaw = parts[7].substring(2); // skip "U:"
            usedCards.clear();
            if (!usedRaw.isBlank()) {
                usedCards.addAll(Arrays.asList(usedRaw.split(",")));
            }

            // APPLY STATE
            model.setCurrentPosition(pos);
            model.setPlayer1Turn("P1".equals(turn));
            model.setRemainingCards(rem);
            model.setTimeLeft(time);
            model.setGameStarted(true);
            model.setPaused(false);

            // scores
            view.player1Score.setText("Score: " + player1Score);
            view.player2Score.setText("Score: " + player2Score);

            // names
            view.player1Title.setText(p1Name);
            view.player2Title.setText(p2Name);

            // portraits
            setPortrait(view.leftPanel, p1Img);
            setPortrait(view.rightPanel, p2Img);

            // cards
            restoreCardsToPanel(view.leftPanel, leftCards);
            restoreCardsToPanel(view.rightPanel, rightCards);

            // discard
            JLabel dLbl = findDiscardLabel1();
            if (dLbl != null && discardFile != null) {
                ImageIcon di = new ImageIcon(model.IMAGE_PATH + discardFile);
                Image scaled = di.getImage().getScaledInstance(100, 150, Image.SCALE_SMOOTH);
                ImageIcon finalIcon = new ImageIcon(scaled);
                finalIcon.setDescription(discardFile);
                dLbl.setIcon(finalIcon);
            }

            // board
            String boardFile = "qtboard" + pos + ".jpg";
            view.boardLabel.setIcon(view.loadScaledImage(model.IMAGE_PATH + boardFile, 900, 600));

            // misc
            view.diceLabel.setText(diceVal);
            view.timeLabel.setText("Time left: " + time + "s");
            view.drawCardLabel1.setText("Draw Card (" + rem + ")");

            updateTurnBorders();
            startTimer();

            System.out.println("RESTORE COMPLETE");

        } catch (Exception ex) {
            ex.printStackTrace();
            msg("Error restoring game: " + ex.getMessage());
        }
    }

    private void setPortrait(Container panel, String fileName) {
        for (Component c : getAllComponents(panel)) {
            if (c instanceof JLabel lbl && lbl.getIcon() instanceof ImageIcon img) {
                if (img.getIconWidth() >= 190 && img.getIconHeight() >= 240) {
                    ImageIcon base = new ImageIcon(model.IMAGE_PATH + fileName);
                    Image scaled = base.getImage().getScaledInstance(200, 250, Image.SCALE_SMOOTH);
                    ImageIcon finalIcon = new ImageIcon(scaled);
                    finalIcon.setDescription(fileName);
                    lbl.setIcon(finalIcon);
                    return;
                }
            }
        }
    }

    private void restoreCardsToPanel(Container panel, String[] cardNames) {
        List<JLabel> labels = getCardLabels(panel);
        for (int i = 0; i < labels.size() && i < cardNames.length; i++) {
            String fname = cardNames[i];
            if (fname == null || fname.isBlank()) continue;
            ImageIcon base = new ImageIcon(model.IMAGE_PATH + fname);
            Image scaled = base.getImage().getScaledInstance(100, 150, Image.SCALE_SMOOTH);
            ImageIcon finalIcon = new ImageIcon(scaled);
            finalIcon.setDescription(fname);
            labels.get(i).setIcon(finalIcon);
        }
    }

    private void msg(String s) {
        JOptionPane.showMessageDialog(view, s);
    }
}
