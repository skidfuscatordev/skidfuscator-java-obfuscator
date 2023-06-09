package dev.skidfuscator.obfuscator.attribute;

public interface Attribute<T> {
    T getBase();

    void set(final T t);
}
