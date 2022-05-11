package dev.skidfuscator.obfuscator.predicate.opaque.impl;

import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.util.RandomUtil;

import java.util.List;

public class IntegerMethodOpaquePredicate implements MethodOpaquePredicate {
    private final SkidGroup group;
    private final int predicate;
    private final int publicPredicate;
    private PredicateFlowGetter getter;
    private PredicateFlowSetter setter;

    public IntegerMethodOpaquePredicate(SkidGroup group) {
        this.group = group;
        this.predicate = RandomUtil.nextInt();
        this.publicPredicate = RandomUtil.nextInt();
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
    public int getPublic() {
        return publicPredicate;
    }

    @Override
    public int getPrivate() {
        return predicate;
    }
}
