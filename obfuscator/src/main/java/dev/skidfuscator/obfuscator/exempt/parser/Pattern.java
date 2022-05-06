package dev.skidfuscator.obfuscator.exempt.parser;

public interface Pattern<T> {
    void parse(final String value);

    boolean check(final T value);

    default String sanitize(final String pattern) {
        return pattern
                .replace("\\", "\\\\")
                .replace("/", "\\/")
                .replace(".", "\\/")
                .replace("$", "\\$");
    }
}
