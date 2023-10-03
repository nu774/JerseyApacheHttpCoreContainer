package acme;

import java.util.Iterator;

public class CharacterGenerator implements Iterable<String>, Iterator<String> {
    final static int COLUMNS = 72;
    final static char first_letter = ' ';
    final static char last_letter = '~';
    char pos = first_letter;

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
        if (pos++ == last_letter)
            pos = first_letter;
        final var data = new char[COLUMNS];
        char ch = pos;
        for (int i = 0; i < COLUMNS; ++i) {
            if ((data[i] = ch++) == last_letter)
                ch = first_letter;
        }
        return new String(data);
    }
}
