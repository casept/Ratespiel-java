import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Scoreboard {
    private List<ScoreboardEntry> scores;
    private String scoresFilepath;
    private Pattern scorePattern;

    public Scoreboard(String scoresFilepath) throws IOException {
        this.scorePattern = Pattern.compile("\"(?<name>.+)\"=\"(?<score>.+)\"", Pattern.UNICODE_CHARACTER_CLASS);
        this.scores = new ArrayList<ScoreboardEntry>();

        // If the file exists, populate the scores array with values from it.
        // If it doesn't, create it.
        this.scoresFilepath = scoresFilepath;
        File scoresFile = new File(scoresFilepath);
        if (!scoresFile.exists()) {
            scoresFile.createNewFile();
        } else { // Only parse the file if it existed already
            Scanner scan = new Scanner(scoresFile);
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                parseLine(line);
            }
            scan.close();
        }
    }

    private ScoreboardEntry parseLine(String line) {
        Matcher matcher = scorePattern.matcher(line);
        matcher.matches(); // Needed to allow using groups
        String name = matcher.group("name");
        String score = matcher.group("score");
        return new ScoreboardEntry(name, Integer.parseInt(score));
    }

    public void addEntry(String name, Integer score) {
        scores.add(new ScoreboardEntry(name, score));
    }

    public List<ScoreboardEntry> getTop5() {
        Collections.sort(scores);
        // Return up to 5 entries.
        // If there are fewer than 5 entries, then all are returned.
        List<ScoreboardEntry> topScores = new ArrayList<ScoreboardEntry>();
        for (Integer i = 0; i < scores.size() && i < 5; i++) {
            topScores.add(scores.get(i));
        }
        return topScores;
    }

    public void save() throws IOException {
        Collections.sort(scores); // So the scoreboard is nicely readable in a text editor

        OutputStream outputStream;
        outputStream = new FileOutputStream(scoresFilepath);
        try (Writer outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8")) {
            for (ScoreboardEntry entry : scores) {
                String scoreLine = "\"" + entry.getName() + "\"" + "=" + "\"" + entry.getScore() + "\"" + "\n";
                outputStreamWriter.write(scoreLine);
            }
        }
        outputStream.close();
    }
}
