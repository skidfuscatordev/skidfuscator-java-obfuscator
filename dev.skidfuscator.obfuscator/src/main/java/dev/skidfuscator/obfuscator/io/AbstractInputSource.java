package dev.skidfuscator.obfuscator.io;

import dev.skidfuscator.obfuscator.Skidfuscator;

public abstract class AbstractInputSource implements InputSource{
    protected final Skidfuscator skidfuscator;

    public AbstractInputSource(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }
}
