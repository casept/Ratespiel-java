class Player implements Comparable<Player> {
    private String name;
    private int score;
    private int questionsAnswered;

    public Player(String name) {
        this.name = name;
        this.score = 0;
        this.questionsAnswered = 0;
    }

    /**
     * @return the number of questions answered by the player.
     */
    public int getQuestionsAnswered() {
        return questionsAnswered;
    }

    /**
     * Increments the number of questions the player has answered by 1.
     */
    public void incrementQuestionsAnswered() {
        this.questionsAnswered = this.questionsAnswered + 1;
    }

    public String getName() {
        return this.name;
    }

    public int getScore() {
        return this.score;
    }

    /**
     * Increments the score of the player by 1.
     */
    public void incrementScore() {
        this.score = this.score + 1;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other == null)
            return false;
        if (getClass() != other.getClass())
            return false;
        Player player = (Player) other;
        return (name == player.name && score == player.score && questionsAnswered == player.questionsAnswered);
    }

    @Override
    public int compareTo(Player otherPlayer) {
        // returns -1 if "this" object is less than "that" object
        // returns 0 if they are equal
        // returns 1 if "this" object is greater than "that" object
        if (this.score < otherPlayer.score) {
            return -1;
        } else if (this.score == otherPlayer.score) {
            return 0;
        } else {
            return 1;
        }
    }
}