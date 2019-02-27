import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// This factory builds a QuestionManager based on the supplied question stream.
class QuestionManagerFactory {
    QuestionManager questionManager;
    // Cache these within the object to avoid recompiling for each line.
    Pattern commentPattern;
    Pattern questionPattern;

    public QuestionManagerFactory(InputStream input) throws IOException {

        // Initialize regexes
        this.commentPattern = Pattern.compile("\\s*#.*"); // Java doesn't have raw string literals, the first "\" is for
                                                          // escaping the second.
        this.questionPattern = Pattern.compile("\"(?<question>.+)\"=\"(?<answer>.+)\"",
                Pattern.UNICODE_CHARACTER_CLASS);

        this.questionManager = new QuestionManager();
        // Split the document into lines and parse each one individually
        BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF8"));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            line = line.replaceAll("(\\r|\\n)", ""); // Strip out all line endings, just to be safe
            parseLine(line);
        }
    }

    private void parseLine(String line) {
        // Use regex to check if it's a comment.
        if (commentPattern.matcher(line).matches()) {
            return;
        } else if (questionPattern.matcher(line).matches()) {
            Matcher matcher = questionPattern.matcher(line);
            matcher.matches(); // Needed to allow using groups
            String question = matcher.group("question");
            String correctAnswer = matcher.group("answer");
            this.questionManager.addQuestion(new Question(question, correctAnswer));
        } else if (line.trim().length() == 0) /* Check for whitespace-only lines */ {
            return;
        } else {
            throw new IllegalQuestionFormatException("Failed to parse line \"" + line + " \"");
        }

    }

    class IllegalQuestionFormatException extends RuntimeException {
        IllegalQuestionFormatException(String msg) {
            super(msg);
        }
    }

    public QuestionManager getQuestionManager() {
        return questionManager;
    }
}