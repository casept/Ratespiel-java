class ScoreboardEntry implements Comparable<ScoreboardEntry> {
    private String name;
    private Integer score;

    public ScoreboardEntry(String name, Integer score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return this.name;
    }

    public Integer getScore() {
        return this.score;
    }

    @Override
    public int compareTo(ScoreboardEntry o) {
        // returns -1 if "this" object is less than "that" object
        // returns 0 if they are equal
        // returns 1 if "this" object is greater than "that" object
        if (this.score < o.score) {
            return -1;
        } else if (this.score == o.score) {
            return 0;
        } else {
            return 1;
        }
    }
}