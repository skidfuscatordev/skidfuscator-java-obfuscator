package dev.skidfuscator.obfuscator.predicate.opaque;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import org.mapleir.asm.ClassNode;

public interface ClassOpaquePredicate extends OpaquePredicate<SkidClassNode> {
    int get();
}
