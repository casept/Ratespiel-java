import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

// This factory builds a QuestionManager based on the supplied YAML stream.
class QuestionManagerFactory {
    QuestionManager questionManager;

    public QuestionManagerFactory(InputStream input) throws IOException {
        questionManager = new QuestionManager();
        Yaml yaml = new Yaml();
        // The questions.yaml file contains each question as a separate java document,
        // so we iterate over them.
        for (Object data : yaml.loadAll(input)) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> dataMap = (HashMap<String, Object>) data;
            questionManager.addQuestion(buildQuestion(dataMap));
        }
    }

    private Question buildQuestion(HashMap<String, Object> dataMap) {
        // I'm too lazy to do proper error handling here.
        // The app will just load the questions from the jar anyways, so the format
        // should be correct.
        // So instead, we'll just define some obviously bad default values to be shown
        // in case something goes wrong.
        String questionText = "ERROR";
        String correctAnswer = "ERROR";
        List<String> answers = new ArrayList<String>();

        // Parse each of the question's entries
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            switch ((String) entry.getKey()) {
            case "question":
                questionText = (String) entry.getValue();
                break;

            case "answers":
                // @SuppressWarnings("unchecked")
                answers = (ArrayList<String>) entry.getValue();
                break;

            case "correct_answer":
                correctAnswer = (String) entry.getValue();
                break;
            }
        }
        // Now that we have all the data we can construct the question object.
        String[] answerArray = answers.toArray(new String[answers.size()]);
        return new Question(questionText, answerArray, correctAnswer);
    }

    public QuestionManager getQuestionManager() {
        return questionManager;
    }
}