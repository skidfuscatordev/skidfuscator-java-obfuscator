package dev.skidfuscator.obfuscator.predicate.opaque;

import dev.skidfuscator.obfuscator.predicate.factory.PredicateFactory;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.Transformer;

import java.util.List;

public interface OpaquePredicate<T> {
    PredicateFlowGetter getGetter();

    void setGetter(final PredicateFlowGetter getter);

    PredicateFlowSetter getSetter();

    void setSetter(final PredicateFlowSetter getter);
}
