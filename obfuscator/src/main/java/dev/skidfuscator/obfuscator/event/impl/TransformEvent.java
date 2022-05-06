package dev.skidfuscator.obfuscator.event.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;

public abstract class TransformEvent extends Event {
    private int changed;

    public TransformEvent(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }

    public void tick() {
        changed++;
    }
}
