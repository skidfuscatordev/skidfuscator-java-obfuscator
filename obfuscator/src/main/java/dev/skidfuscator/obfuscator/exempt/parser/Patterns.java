package dev.skidfuscator.obfuscator.exempt.parser;

import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public enum Patterns {
    /**
     * (    -> Open group
     * ?:   -> Look ahead
     * \\/  -> All matching a /
     * $    -> Only if they're the last character
     * )    -> Close group
     */
    PACKAGE_SPECIFIC(Pattern.compile("(?:\\/$)")),
    PACKAGE_WILDCARD(Pattern.compile("(?:\\/\\*\\*$)"));


    private final Pattern pattern;

    Patterns(Pattern pattern) {
        this.pattern = pattern;
    }
}
