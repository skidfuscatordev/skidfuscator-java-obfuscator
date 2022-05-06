package dev.skidfuscator.obfuscator.event.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;

public abstract class Event {
    private final Skidfuscator skidfuscator;

    public Event(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }

    public Skidfuscator getSkidfuscator() {
        return skidfuscator;
    }
}
