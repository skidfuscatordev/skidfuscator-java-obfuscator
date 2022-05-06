package dev.skidfuscator.obfuscator.predicate;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import org.mapleir.ir.cfg.BasicBlock;

public interface PredicateAnalysis {
    BlockOpaquePredicate getBlockPredicate(final SkidMethodNode methodNode);

    MethodOpaquePredicate getMethodPredicate(final SkidGroup group);

    ClassOpaquePredicate getClassPredicate(final SkidClassNode classNode);

    ClassOpaquePredicate getClassStaticPredicate(final SkidClassNode classNode);

}
