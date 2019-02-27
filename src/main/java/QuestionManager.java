import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// QuestionManager is a container for questions that encapsulates actions like drawing a fresh, random question.
class QuestionManager {
    private List<Question> questions;

    public QuestionManager() {
        questions = new ArrayList<Question>();
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public Question getRandomUnseenQuestion() {
        Random rand = new Random();
        int randomIndex = rand.nextInt(questions.size());
        Question question = questions.get(randomIndex);
        // Remove the question from the list so it isn't offered again.
        questions.remove(question);
        return question;
    }

    public boolean hasQuestions() {
        return !questions.isEmpty();
    }
}