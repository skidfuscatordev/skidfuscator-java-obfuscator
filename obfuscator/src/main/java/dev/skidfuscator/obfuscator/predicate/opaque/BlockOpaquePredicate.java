package dev.skidfuscator.obfuscator.predicate.opaque;

import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;

public interface BlockOpaquePredicate extends OpaquePredicate<SkidMethodNode> {
    int get(final SkidBlock t);
}
