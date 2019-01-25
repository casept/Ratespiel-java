import java.util.Iterator;

class AlphabetIterator implements Iterator<Character> {

    char nextLetter;

    public AlphabetIterator() {
        this.nextLetter = 'A';
    }

    public boolean hasNext() {
        if (nextLetter == '[') { // One character past the uppercase ASCII block
            return false;
        } else {
            return true;
        }
    }

    public Character next() {
        // The ASCII letters are stored in alphabetic order in the Unicode block.
        char currentLetter = this.nextLetter;
        nextLetter = (char) (currentLetter + 1);
        return currentLetter;
    }
}