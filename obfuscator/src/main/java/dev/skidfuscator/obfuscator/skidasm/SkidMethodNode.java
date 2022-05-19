package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.SkidFlowGraphDumper;
import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import lombok.Getter;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SkidMethodNode extends MethodNode {
    private ControlFlowGraph cfg;
    private final BlockOpaquePredicate flowPredicate;
    private final Skidfuscator skidfuscator;
    private final List<SkidInvocation> invokers;
    private MethodOpaquePredicate predicate;

    private SkidGroup group;

    public SkidMethodNode(org.objectweb.asm.tree.MethodNode node, ClassNode owner, Skidfuscator skidfuscator) {
        super(node, owner);
        this.skidfuscator = skidfuscator;
        this.invokers = new ArrayList<>();
        this.flowPredicate = skidfuscator
                .getPredicateAnalysis()
                .getBlockPredicate(this);
    }

    public int getBlockPredicate(final SkidBlock block) {
        return flowPredicate.get(block);
    }

    public void setGroup(SkidGroup group) {
        this.group = group;
        this.predicate = skidfuscator
                .getPredicateAnalysis()
                .getMethodPredicate(group);
    }

    public boolean isInit() {
        return node.name.equals("<init>");
    }

    public boolean isClinit() {
        return node.name.equals("<clinit>");
    }


    /**
     * Recomputes the ControlFlowGraph from scratch
     */
    public void recomputeCfg() {
        // Remove key then recompute it
        this.skidfuscator.getIrFactory().remove(this);
        this.cfg = skidfuscator.getIrFactory().getFor(this);
    }

    public ControlFlowGraph getCfg() {
        /* Lazy computing to improve import performance */
        if (cfg == null) {
            cfg = skidfuscator.getIrFactory().getFor(this);
        }
        return cfg;
    }

    /**
     * Dumps the control flow graph to the method node
     */
    public void dump() {
        new SkidFlowGraphDumper(skidfuscator, this.getCfg(), this).dump();
    }

    public void addInvocation(final SkidInvocation invocation) {
        this.invokers.add(invocation);
    }

    public List<SkidInvocation> getInvokers() {
        return invokers;
    }

    public SkidClassNode getParent() {
        return (SkidClassNode) owner;
    }
}
