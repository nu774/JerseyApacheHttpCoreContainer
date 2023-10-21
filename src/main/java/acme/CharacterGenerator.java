package acme;

import java.util.Iterator;

public class CharacterGenerator implements Iterable<String>, Iterator<String> {
    final static int COLUMNS = 72;
    final static char FIRST_LETTER = ' ';
    final static char LAST_LETTER = '~';
    char pos = FIRST_LETTER;

    @Override
    public Iterator<String> iterator() {
        return this;
    }
    @Override
    public boolean hasNext() {
        return true;
    }
    @Override
    public String next() {
        pos = increment(pos);
        final var sb = new StringBuilder();
        char ch = pos;
        for (int i = 0; i < COLUMNS; ++i) {
            sb.append(ch);
            ch = increment(ch);
        }
        return sb.toString();
    }
    private char increment(char ch) {
        if (ch++ == LAST_LETTER)
            ch = FIRST_LETTER;
        return ch;
    }
}
