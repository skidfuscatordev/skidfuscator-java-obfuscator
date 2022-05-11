package dev.skidfuscator.obfuscator.predicate.opaque;

import dev.skidfuscator.obfuscator.skidasm.SkidGroup;

public interface MethodOpaquePredicate extends OpaquePredicate<SkidGroup> {
    int getPublic();

    int getPrivate();
}
