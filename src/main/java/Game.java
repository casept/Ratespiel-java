import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
    private static final int NUM_PLAYERS = 2; // only 2 players can participate according to the requirements
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
        this.questionManager = new QuestionManager();
        pickQuestions();

    }

    private void pickQuestions() {
        for (int i = 0; i < 10000; i++) {
            questionManager.addQuestion(new Question("Wann hat Edison die Glühbirne erfunden?",
                    new String[] { "1860", "1900", "1879", "1840" }, "1879"));
        }
        // TODO: Parse a text file containing questions, pick some at random
    }

    private void createWindow() {
        this.frame = new JFrame("Ratespiel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BoxLayout rootLayout = new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS);
        frame.getContentPane().setLayout(rootLayout);
        frame.setMinimumSize(new Dimension(400, 200));
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
        for (int i = 0; i < 5; i++) {
            // 2 Players who take turns
            for (int j = 0; j < 2; j++) {
                Player currentPlayer = playerManager.next();
                Question currentQuestion = questionManager.getRandomUnseenQuestion();
                takeTurn(currentPlayer, currentQuestion, i + 1 /* Zero-indexed */, 5, false);
            }
        }
        // If there's a stalemate, keep posing questions in turns to each player.
        if (playerManager.isStalemate()) {
            while (true) {
                takeTurn(playerManager.next(), questionManager.getRandomUnseenQuestion(), 0,
                        0 /* Ignored by method when resolving a stalemate */, true);
                if (!playerManager.isStalemate()) {
                    break;
                }
            }
        }

        // Crown our winner and ask whether the player wishes to play again
        congratulateWinner();

        int dialogResult = JOptionPane.showConfirmDialog(null, "Neues Spiel anfangen?", "Neues Spiel",
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
     * @param player           The player who guesses.
     * @param question         The question to guess.
     * @param questionNo       The index of the current question, used to display to
     *                         how many questions remain. If the question is a
     *                         stalemate breaker, this isn't displayed to the user.
     * @param totalQuestions   The total amount of questions the player will be
     *                         asked this session (without stalemate breakers). If
     *                         the question is a stalemate breaker, this will be
     *                         ignored.
     * @param stalemateBreaker Tells the user whether this is a question for
     *                         breaking a stalemate and therefore outside the
     *                         regular amount of questions.
     */
    private void takeTurn(Player player, Question question, int questionNo, int totalQuestions,
            boolean isStalemateBreaker) {

        ArrayBlockingQueue<String> playersAnswer = new ArrayBlockingQueue<String>(1);
        // This class implements the callbacks that the answer buttons call when
        // pressed.
        class ButtonHandler implements ActionListener {
            // Keep track of which answer our button is for
            String answer;
            ArrayBlockingQueue<String> playersAnswer;

            public ButtonHandler(String answer, ArrayBlockingQueue<String> playersAnswer) {
                this.answer = answer;
                this.playersAnswer = playersAnswer;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                // Relay the player's answer to the main thread.
                try {
                    playersAnswer.put(answer);
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
        if (!isStalemateBreaker) {
            questionLabel = new JLabel("Frage " + Integer.toString(questionNo) + "/" + Integer.toString(totalQuestions)
                    + ": " + question.getQuestion());
        } else {
            questionLabel = new JLabel("Stichfrage: " + question.getQuestion());
        }
        panel.add(playerNameLabel);
        panel.add(questionLabel);
        frame.add(panel);

        AlphabetIterator alphabet = new AlphabetIterator();
        JPanel answersPanel = new JPanel(new GridLayout());
        for (String answer : question.getAnswers()) {
            JButton button = new JButton(Character.toString(alphabet.next()) + ": " + answer);
            ButtonHandler buttonHandler = new ButtonHandler(answer, playersAnswer);
            button.addActionListener(buttonHandler);
            answersPanel.add(button);
        }
        panel.add(answersPanel);
        frame.setVisible(true);

        // Wait for the player to pick an answer
        String answer;
        try {
            answer = playersAnswer.take();
        } catch (InterruptedException e1) {
            return;
        }

        // TODO: Tell the player whether he was correct
        if (question.isCorrect(answer)) {
            player.incrementScore();
        }
        player.incrementQuestionsAnswered();

        // Delete the question from the screen
        frame.remove(panel);
    }

    private void congratulateWinner() {
        Player winner = playerManager.getWinner();

        // Show the player a nice picture for their troubles
        try {
            BufferedImage image = ImageIO.read(Game.class.getResourceAsStream("winner.png"));
            ImageIcon imageIcon = new ImageIcon(image);
            JOptionPane.showMessageDialog(null, "Spieler " + winner.getName() + " hat gewonnen!", "YOU'RE WINNER!",
                    JOptionPane.DEFAULT_OPTION, imageIcon);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Spieler " + winner.getName() + " hat gewonnen!");
        }
    }

    /**
     * Disposes of any windows or other resources used by this class.
     **/
    public void dispose() {
        frame.dispose();
    }
}