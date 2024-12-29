package dev.skidfuscator.pureanalysis;

public enum Purity {
    IMPURE,
    PURE,
    MUD;

    boolean isPure() {
        return this == PURE || this == MUD;
    }
}
