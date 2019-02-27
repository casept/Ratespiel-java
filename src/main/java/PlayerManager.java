import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class PlayerManager implements Iterator<Player> {
    private ArrayList<Player> players;
    private int nextPlayerIndex = 0;

    public PlayerManager() {
        players = new ArrayList<Player>();
    }

    @Override
    public boolean hasNext() {
        // We always supply the next player in a loop, round-robin style
        // That means that we never run out of players.
        return true;
    }

    @Override
    public Player next() {
        Player player = players.get(nextPlayerIndex);
        if (nextPlayerIndex + 1 == players.size()) {
            nextPlayerIndex = 0;
        } else {
            nextPlayerIndex++;
        }
        return player;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public boolean isStalemate() {
        // Record the scores of all players and sort them.
        List<Integer> scores = new ArrayList<Integer>();
        for (Player player : players) {
            scores.add(player.getScore());
        }
        Collections.sort(scores, Collections.reverseOrder());
        // If there are 2 or more identical top entries we have a stalemate.
        return (scores.get(0) == scores.get(1));
    }

    public Player getWinner() {
        // The cast is guaranteed to be safe.
        @SuppressWarnings("unchecked")
        ArrayList<Player> playerCopy = (ArrayList<Player>) players.clone();
        Collections.sort(playerCopy, Collections.reverseOrder());
        return playerCopy.get(0);
    }

    public Player getLoser() {
        // The cast is guaranteed to be safe.
        @SuppressWarnings("unchecked")
        ArrayList<Player> playerCopy = (ArrayList<Player>) players.clone();
        Collections.sort(playerCopy);
        return playerCopy.get(0);
    }
}