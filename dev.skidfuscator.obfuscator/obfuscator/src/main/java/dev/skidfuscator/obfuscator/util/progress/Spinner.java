package dev.skidfuscator.obfuscator.util.progress;

/**
 * @author Ghast
 * @since 07/02/2021
 * Artemis © 2021
 */
public class Spinner<T> {
    private final T[] values;
    private int index;

    public Spinner(T... values) {
        this.values = values;
        this.index = 0;
    }

    public T next() {
        return values[index = (++index % values.length)];
    }
}