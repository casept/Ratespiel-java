import java.util.Random;

class Question {
    private String question;
    private String[] answers;
    private String correctAnswer;

    public Question(String question, String[] answers, String correctAnswer) {
        this.question = question;
        this.answers = answers;
        this.correctAnswer = correctAnswer;
    }

    public String getQuestion() {
        return this.question;
    }

    /**
     * Returns the answers in random order.
     * 
     * @return The answers in random order.
     */
    public String[] getAnswers() {
        Random rand = new Random();
        String[] randomizedAnswers = new String[answers.length];
        Boolean[] answersUsed = new Boolean[answers.length];
        // Have to intialize the array to all false manually.
        for (int i = 0; i < answersUsed.length; i++) {
            answersUsed[i] = false;
        }
        int i = 0;
        while (i < answers.length) {
            int nextAnswer = rand.nextInt(answers.length);
            if (answersUsed[nextAnswer] == false) {
                randomizedAnswers[i] = answers[nextAnswer];
                answersUsed[nextAnswer] = true;
                i++;
            }
            // Do nothing and simply try another number if the answer's already used
        }
        return randomizedAnswers;
    }

    public Boolean isCorrect(String answer) {
        if (answer == correctAnswer) {
            return true;
        }
        return false;
    }
}