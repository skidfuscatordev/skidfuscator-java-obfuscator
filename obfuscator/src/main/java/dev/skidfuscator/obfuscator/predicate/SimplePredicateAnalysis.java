package dev.skidfuscator.obfuscator.predicate;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFactory;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimplePredicateAnalysis implements PredicateAnalysis {
    private final Skidfuscator skidfuscator;

    private final Map<SkidMethodNode, BlockOpaquePredicate> blockOpaqueMap = new ConcurrentHashMap<>();
    private final Map<SkidGroup, MethodOpaquePredicate> methodOpaqueMap = new ConcurrentHashMap<>();
    private final Map<SkidClassNode, ClassOpaquePredicate> classOpaqueMap = new ConcurrentHashMap<>();
    private final Map<SkidClassNode, ClassOpaquePredicate> classStaticOpaqueMap = new ConcurrentHashMap<>();

    private PredicateFactory<ClassOpaquePredicate, SkidClassNode> classOpaqueFactory;
    private PredicateFactory<ClassOpaquePredicate, SkidClassNode> classStaticOpaqueFactory;
    private PredicateFactory<MethodOpaquePredicate, SkidGroup> methodOpaqueFactory;
    private PredicateFactory<BlockOpaquePredicate, SkidMethodNode> blockOpaqueFactory;

    public SimplePredicateAnalysis(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }

    @Override
    public BlockOpaquePredicate getBlockPredicate(SkidMethodNode method) {
        return blockOpaqueMap.computeIfAbsent(method, e -> blockOpaqueFactory.build(e));
    }

    @Override
    public MethodOpaquePredicate getMethodPredicate(SkidGroup group) {
        return methodOpaqueMap.computeIfAbsent(group, e -> methodOpaqueFactory.build(e));
    }

    @Override
    public ClassOpaquePredicate getClassPredicate(SkidClassNode classNode) {
        return classOpaqueMap.computeIfAbsent(classNode, e -> classOpaqueFactory.build(e));
    }

    @Override
    public ClassOpaquePredicate getClassStaticPredicate(SkidClassNode classNode) {
        return classStaticOpaqueMap.computeIfAbsent(classNode, e -> classStaticOpaqueFactory.build(e));
    }

    public static class Builder {
        private Skidfuscator skidfuscator;
        private PredicateFactory<ClassOpaquePredicate, SkidClassNode> classOpaqueFactory;
        private PredicateFactory<ClassOpaquePredicate, SkidClassNode> classStaticOpaqueFactory;
        private PredicateFactory<MethodOpaquePredicate, SkidGroup> methodOpaqueFactory;
        private PredicateFactory<BlockOpaquePredicate, SkidMethodNode> blockOpaqueFactory;

        public Builder skidfuscator(Skidfuscator skidfuscator) {
            this.skidfuscator = skidfuscator;
            return this;
        }

        public Builder classOpaqueFactory(PredicateFactory<ClassOpaquePredicate, SkidClassNode> classOpaqueFactory) {
            this.classOpaqueFactory = classOpaqueFactory;
            return this;
        }

        public Builder classStaticOpaqueFactory(PredicateFactory<ClassOpaquePredicate, SkidClassNode> classOpaqueFactory) {
            this.classStaticOpaqueFactory = classOpaqueFactory;
            return this;
        }

        public Builder methodOpaqueFactory(PredicateFactory<MethodOpaquePredicate, SkidGroup> methodOpaqueFactory) {
            this.methodOpaqueFactory = methodOpaqueFactory;
            return this;
        }

        public Builder blockOpaqueFactory(PredicateFactory<BlockOpaquePredicate, SkidMethodNode> blockOpaqueFactory) {
            this.blockOpaqueFactory = blockOpaqueFactory;
            return this;
        }

        public SimplePredicateAnalysis build() {
            final SimplePredicateAnalysis analysis = new SimplePredicateAnalysis(skidfuscator);
            analysis.classOpaqueFactory = classOpaqueFactory;
            analysis.classStaticOpaqueFactory = classStaticOpaqueFactory;
            analysis.methodOpaqueFactory = methodOpaqueFactory;
            analysis.blockOpaqueFactory = blockOpaqueFactory;
            return analysis;
        }
    }
}
