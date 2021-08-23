package dev.skidfuscator.obf.transform.seed;

import dev.skidfuscator.obf.transform.yggdrasil.SkidMethod;

public abstract class AbstractSeed<T> implements Seed<T> {
    protected final SkidMethod parent;
    protected T privateSeed;
    protected T publicSeed;

    public AbstractSeed(SkidMethod parent, T privateSeed, T publicSeed) {
        this.parent = parent;
        this.privateSeed = privateSeed;
        this.publicSeed = publicSeed;
    }
}
