package dev.skidfuscator.obfuscator.predicate.opaque;

import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import org.mapleir.asm.ClassNode;

public interface ClassOpaquePredicate extends OpaquePredicate<SkidClassNode> {
    int get();

    @Override
    PredicateFlowGetter getGetter();

    @Override
    void setGetter(final PredicateFlowGetter getter);

    @Override
    PredicateFlowSetter getSetter();

    @Override
    void setSetter(final PredicateFlowSetter getter);
}
