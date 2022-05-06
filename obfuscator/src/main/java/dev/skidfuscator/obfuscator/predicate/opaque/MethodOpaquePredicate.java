package dev.skidfuscator.obfuscator.predicate.opaque;

import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;

public interface MethodOpaquePredicate extends OpaquePredicate<SkidGroup> {
    int get();
}
