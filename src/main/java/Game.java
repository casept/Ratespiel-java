import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

class Game {
    private JFrame frame;
    private static final int NUM_PLAYERS = 2; // Only 2 players can participate according to the requirements.
    private static final int NUM_QUESTIONS_PER_PLAYER = 5; // Each player gets 5 questions.
    private PlayerManager playerManager;
    private QuestionManager questionManager;

    public Game() {
        createWindow();
        this.playerManager = new PlayerManager();
        String[] names = getPlayerNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            playerManager.addPlayer(new Player(name));
        }
        InputStream questionStream;
        try {
            // Parse the questions file to a QuestionManager.
            questionStream = Game.class.getResourceAsStream("questions.txt" /* This file is embedded in the jar */);
            QuestionManagerFactory questionManagerFactory = new QuestionManagerFactory(questionStream);
            questionManager = questionManagerFactory.getQuestionManager();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Fragen konnten nicht geladen werden",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void createWindow() {
        this.frame = new JFrame("Ratespiel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BoxLayout rootLayout = new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS);
        frame.getContentPane().setLayout(rootLayout);
        frame.setMinimumSize(new Dimension(600, 200));
        // Set the window icon and title
        ImageIcon icon = new ImageIcon(getClass().getResource("icon.png"));
        frame.setIconImage(icon.getImage());
        frame.setTitle("Ratespiel");
    }

    private String[] getPlayerNames() {
        String[] names = new String[NUM_PLAYERS];

        /*
         * This blocking queue is used as a way to synchronize the callbacks with the
         * bottom part of the function. That way, the function won't return until the
         * callback has fired and the user has input valid names.
         */
        BlockingQueue<Boolean> done = new ArrayBlockingQueue<Boolean>(1);

        // Draw player input dialog in a separate panel
        JPanel panel = new JPanel(new FlowLayout());
        // Can't set a BoxLayout during JPanel construction, as it's constructor
        // requires the object it'll be part of.
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        frame.add(panel);

        // Each player also gets a separate panel (makes it easier to manage placement
        // of the label)
        // TODO: Fix, looks very ugly
        JTextField[] playerTextFields = new JTextField[NUM_PLAYERS];
        for (int i = 0; i < NUM_PLAYERS; i++) {
            JPanel playerPanel = new JPanel(new GridLayout(1, 2));
            JLabel playerLabel = new JLabel("Name von Spieler #" + Integer.toString(i + 1) + " :");
            playerTextFields[i] = new JTextField();

            playerPanel.add(playerLabel);
            playerPanel.add(playerTextFields[i]);
            panel.add(playerPanel);
        }

        // Create a "next" button
        JButton nextButton = new JButton("Weiter");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < NUM_PLAYERS; i++) {
                    names[i] = playerTextFields[i].getText();
                }

                // Validate the names
                // TODO: Make sure players have distinct names
                for (String name : names) {
                    if (name.length() == 0) {
                        JOptionPane.showMessageDialog(null, "Bitte gib beide Namen an!", "Namen nicht valide",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                try {
                    done.put(true);
                } catch (InterruptedException e1) {
                    // This exception only occurs when the thread was interrupted during excecution
                    // That usually happens because the user killed us, so we should pack up and
                    // leave
                    return;
                }
            }
        });
        panel.add(nextButton);
        frame.setVisible(true);

        // Destroy the input screen once names have been input
        try {
            done.take(); // This operation blocks until the callback validates the input
        } catch (InterruptedException e1) {
            return names;
        }
        frame.remove(panel);
        return names;
    }

    /**
     * Starts the quiz game
     * 
     * @return Whether the player wishes to start a new game after this one is over
     **/
    public boolean playGame() {
        // Each player answers 5 questions
        for (int i = 0; i < NUM_QUESTIONS_PER_PLAYER; i++) {
            // 2 Players who take turns
            for (int j = 0; j < 2; j++) {
                Player currentPlayer = playerManager.next();
                checkQuestionsLeft();
                Question currentQuestion = questionManager.getRandomUnseenQuestion();
                takeTurn(currentPlayer, currentQuestion, i + 1 /* Zero-indexed */, NUM_QUESTIONS_PER_PLAYER);
            }
        }

        // Crown our winner and ask whether the player wishes to play again
        showResults();

        int dialogResult = JOptionPane.showConfirmDialog(null, "Weiterspielen?", "Weiterspielen?",
                JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * The given player takes a turn at guessing the answer to the question.
     *
     * @param player         The player who guesses.
     * @param question       The question to guess.
     * @param questionNo     The index of the current question, used to display to
     *                       how many questions remain. If the question is a
     *                       stalemate breaker, this isn't displayed to the user.
     * @param totalQuestions The total amount of questions the player will be asked
     *                       this session (without stalemate breakers). If the
     *                       question is a stalemate breaker, this will be ignored.
     */
    private void takeTurn(Player player, Question question, int questionNo, int totalQuestions) {

        // Use this to make the main thread block until the callback is triggered by the
        // player entering their answer.
        ArrayBlockingQueue<String> playersAnswerQueue = new ArrayBlockingQueue<String>(1);
        // This class implements the callbacks that the answer box calls.
        class TextFieldHandler implements ActionListener {
            ArrayBlockingQueue<String> playersAnswerQueue;

            public TextFieldHandler(ArrayBlockingQueue<String> playersAnswerQueue) {
                this.playersAnswerQueue = playersAnswerQueue;
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                JTextField eventSource = (JTextField) e.getSource();
                // Relay the player's answer to the main thread.
                try {
                    playersAnswerQueue.put(eventSource.getText());
                } catch (InterruptedException e1) {
                    /*
                     * This exception only occurs when the thread was interrupted during excecution.
                     * That usually happens because the user killed us, so we should pack up and
                     * leave.
                     */
                    return;
                }
            }

        }

        // Draw the question
        JPanel panel = new JPanel(new FlowLayout());
        // Can't set a BoxLayout during JPanel construction, as it's constructor
        // requires the object it'll be part of.
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel playerNameLabel = new JLabel("Spieler \"" + player.getName() + "\" ist dran");
        JLabel questionLabel;
        questionLabel = new JLabel("Frage " + Integer.toString(questionNo) + "/" + Integer.toString(totalQuestions)
                + ": " + question.getQuestion());
        panel.add(playerNameLabel);
        panel.add(questionLabel);
        frame.add(panel);

        panel.add(new JLabel("\nAntwort:"));
        JTextField answerField = new JTextField(20);
        TextFieldHandler textFieldHandler = new TextFieldHandler(playersAnswerQueue);
        answerField.addActionListener(textFieldHandler);
        panel.add(answerField);

        frame.setVisible(true);

        // Block until the player enters an answer
        String answer;
        try {
            answer = playersAnswerQueue.take();
        } catch (InterruptedException e1) {
            return;
        }

        // TODO: Tell the player whether he was correct
        if (question.isCorrect(answer)) {
            player.incrementScore();
        } else {
            player.decrementScore();
        }
        player.incrementQuestionsAnswered();

        // Delete the question from the screen
        frame.remove(panel);
    }

    /**
     * Tells the player there are no more questions left in the QuestionManager.
     */
    private void checkQuestionsLeft() {
        // FIXME: Clarify requirement (what to do when there are no more questions?)
        if (!questionManager.hasQuestions()) {
            JOptionPane.showMessageDialog(null,
                    "Es sind keine Fragen mehr zur verfügung!\n Das Spiel wird jetzt beendet.", "Keine Fragen mehr",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void showResults() {
        Player winner = playerManager.getWinner();
        Player loser = playerManager.getLoser();

        // Show the players nice pictures for their troubles
        try {
            BufferedImage winImage = ImageIO.read(Game.class.getResourceAsStream("winner.png"));
            ImageIcon winIcon = new ImageIcon(winImage);
            BufferedImage loseImage = ImageIO.read(Game.class.getResourceAsStream("loser.jpeg"));
            ImageIcon loseIcon = new ImageIcon(loseImage);

            JPanel resultsPanel = new JPanel(new GridLayout(2, 2));
            resultsPanel.add(new JLabel(
                    "Spieler " + winner.getName() + " hat mit " + winner.getScore() + " Punkt(en) gewonnen!"));
            resultsPanel.add(new JLabel(winIcon));
            resultsPanel.add(
                    new JLabel("Spieler " + loser.getName() + " hat mit " + loser.getScore() + " Punkt(en) verloren!"));
            resultsPanel.add(new JLabel(loseIcon));

            frame.add(resultsPanel);
            frame.setVisible(true);

        } catch (IOException e) {
            // Couldn't load image for some reason, shouldn't happen as they're part of the
            // jar
            // TODO: Fallback to no images instead
            JOptionPane.showMessageDialog(null,
                    "Kritische Spieldateien konnten nicht geladen werden!\n Das Spiel wird jetzt beendet.",
                    "Kritischer Fehler", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Disposes of any windows or other resources used by this class.
     **/
    public void dispose() {
        frame.dispose();
    }
}