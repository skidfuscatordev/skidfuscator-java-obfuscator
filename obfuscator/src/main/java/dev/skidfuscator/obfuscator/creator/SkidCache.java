package dev.skidfuscator.obfuscator.creator;

import org.mapleir.context.IRCache;

public class SkidCache extends IRCache {
    public SkidCache() {
        super(SkidFlowGraphBuilder::build);
    }
}
