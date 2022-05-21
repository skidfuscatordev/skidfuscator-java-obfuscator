package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.context.IRCache;

public class SkidCache extends IRCache {
    public SkidCache(final Skidfuscator skidfuscator) {
        super(e -> SkidFlowGraphBuilder.build(skidfuscator, e));
    }
}
