package dev.skidfuscator.obfuscator.predicate.opaque.impl;

import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.util.RandomUtil;

import java.util.List;

public class IntegerClassOpaquePredicate implements ClassOpaquePredicate {
    private final SkidClassNode classNode;
    private final int predicate;
    private PredicateFlowGetter getter;
    private PredicateFlowSetter setter;

    public IntegerClassOpaquePredicate(SkidClassNode classNode) {
        this.classNode = classNode;
        this.predicate = RandomUtil.nextInt();
    }

    @Override
    public PredicateFlowGetter getGetter() {
        return getter;
    }

    @Override
    public void setGetter(PredicateFlowGetter getter) {
        this.getter = getter;
    }

    @Override
    public PredicateFlowSetter getSetter() {
        return setter;
    }

    @Override
    public void setSetter(PredicateFlowSetter setter) {
        this.setter = setter;
    }

    @Override
    public int get() {
        return predicate;
    }
}
