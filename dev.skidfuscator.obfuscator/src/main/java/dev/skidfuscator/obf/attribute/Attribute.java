package dev.skidfuscator.obf.attribute;

public interface Attribute<T> {
    T getBase();

    void set(final T t);
}