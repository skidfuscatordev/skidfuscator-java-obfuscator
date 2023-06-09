package dev.skidfuscator.obfuscator.attribute;
/**
 * @author Ghast
 * @since 12/02/2021
 * Artemis © 2021
 */
public class StandardAttribute<T> implements Attribute<T> {
    private T t;

    public StandardAttribute(final T t) {
        this.t = t;
    }

    @Override
    public T getBase() {
        return t;
    }

    @Override
    public void set(final T t) {
        this.t = t;
    }
}
