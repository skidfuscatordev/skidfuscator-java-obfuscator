package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.attribute.Attribute;
import dev.skidfuscator.obfuscator.attribute.AttributeKey;
import dev.skidfuscator.obfuscator.attribute.AttributeMap;
import dev.skidfuscator.obfuscator.creator.SkidFlowGraphDumper;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlockFactory;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import dev.skidfuscator.obfuscator.transform.exempt.MethodExempt;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import lombok.Getter;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.edges.ImmediateEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.utils.CFGUtils;
import org.objectweb.asm.Type;

import java.util.*;

@Getter
public class SkidMethodNode extends MethodNode {
    private SkidControlFlowGraph cfg;
    private final BlockOpaquePredicate flowPredicate;
    private final Skidfuscator skidfuscator;
    private final List<SkidInvocation> invokers;
    private MethodOpaquePredicate predicate;
    private SkidGroup group;
    private transient BasicBlock initBlock;
    private final AttributeMap attributes;

    private transient boolean extruded;

    public SkidMethodNode(org.objectweb.asm.tree.MethodNode node, ClassNode owner, Skidfuscator skidfuscator) {
        super(node, owner);
        this.skidfuscator = skidfuscator;
        this.invokers = new ArrayList<>();
        this.flowPredicate = skidfuscator
                .getPredicateAnalysis()
                .getBlockPredicate(this);
        this.attributes = new AttributeMap();
    }

    public int getBlockPredicate(final SkidBlock block) {
        return flowPredicate.get(block);
    }

    public <T> Attribute<T> getAttribute(AttributeKey attributeKey) {
        return attributes.poll(attributeKey);
    }

    public <T> void setAttribute(AttributeKey attributeKey, T value) {
        attributes.get(attributeKey).set(value);
    }

    /**
     * @return Method hierarchy group to which this group belongs to
     */
    public SkidGroup getGroup() {
        return group;
    }

    /**
     * Sets the method hierarchy group to which this group belongs to.
     * Warning: this method is dangerous if improperly used.
     *
     * @param group SkidGroup to which the method belongs to.
     */
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

    /**
     * This method is lazy computed to save performance.
     *
     * @return  Returns the control flow graph of the method.
     */
    public SkidControlFlowGraph getCfg() {
        /* Lazy computing to improve import performance */
        if (cfg == null) {
            cfg = skidfuscator.getIrFactory().getFor(this);
        }
        return cfg;
    }

    public void setEntryBlock(BasicBlock initBlock) {
        assert isClinit() : "Cannot force entry block on anything but clinit";
        this.initBlock = initBlock;
    }

    /**
     * @return Returns the entry point of this specific method
     */
    public BasicBlock getEntryBlock() {
        if (initBlock != null) {
            return initBlock;
        }

        final BasicBlock fakeEntry = this.getCfg()
                .getEntries()
                .iterator()
                .next();

        /*
         * If the method is static OR is not the <init> method
         * we can safely assume it is the first block in the
         * control flow graph as it will be in an initialized
         * environment
         *
         * TODO: Fix other ting
         */
        if (isStatic() || !isInit())
            return (initBlock = fakeEntry);

        /*
         * If the method is an init method, we should be considering
         * as an entry method the method which calls the super
         * statement and not anything before the such as it
         * would be dangerous.
         */
        final Expr initiator = cfg.allExprStream()
                .filter(e -> e instanceof VirtualInvocationExpr)
                .map(e -> (VirtualInvocationExpr) e)
                .filter(e -> {
                    /*
                     * Reject any calls which aren't an <init> call.
                     * We're only looking for java/lang/Object#<init>
                     * or similar calls to super
                     */
                    if (!e.getName().equals("<init>"))
                        return false;

                    if (!e.isSpecialCall())
                        return false;

                    //System.out.println("----> " + CodeUnit.print(e));

                    /*
                     * Every super() call has to call to have the following
                     * requirements:
                     *   - More than 0 children
                     *   - Children of index 0 must be a "this." instance call
                     *
                     * Hopefully these requirements are strict enough to
                     * properly find it without any false matches.
                     */
                    if (e.getArgumentExprs().length == 0) {
                        System.out.println("Rippp");
                        return false;
                    }

                    //System.out.println("\\_> " + Arrays.toString(e.getArgumentExprs()));
                    final Expr children0 = e.getArgumentExprs()[0];

                    if (!(children0 instanceof VarExpr)) {
                        System.out.println("Index 0 not var expr :((( (" + children0.toString()
                                + " - " + children0.getClass().getName() + ")");
                        return false;
                    }
                    final VarExpr supposedInstantiation = (VarExpr) children0;
                    return supposedInstantiation.getType().equals(this.getParent().getType());
                })
                .findFirst()
                .orElse(null);
        //System.out.println("<--------> " + this.getOwner() + "#" + getName());

        if (initiator == null) {
            throw new IllegalStateException(
                    "Failed to find super() initialization call in method initializer of method "
                            + this.getOwner() + "#" + this.getName() + "(" + this.getDesc() + ")"
                            + "\n--------------------------------------------------------------------\n"
                            + this.getCfg().toString()
                            + "\n--------------------------------------------------------------------\n"
            );
        }


        final Stmt initStmt = initiator.getRootParent();
        BasicBlock initBlock = initiator.getBlock();
        BasicBlock next;

        final int index = initBlock.indexOf(initStmt);

        if (initBlock.size() > index) {
            next = initBlock;
            initBlock = CFGUtils.splitBlock(
                    SkidBlockFactory.v(skidfuscator),
                    cfg,
                    initBlock,
                    index + 1
            );
        } else {
            next = cfg.getImmediate(initBlock);
        }

        cfg.removeEdge(new ImmediateEdge<>(initBlock, next));

        final BasicBlock newEntry = SkidBlockFactory
                .v(skidfuscator)
                .block()
                .cfg(cfg)
                .build();

        final UnconditionalJumpEdge<BasicBlock> edgeStart = new UnconditionalJumpEdge<>(
                initBlock,
                newEntry
        );
        initBlock.add(new UnconditionalJumpStmt(newEntry, edgeStart));
        cfg.addEdge(edgeStart);

        final UnconditionalJumpEdge<BasicBlock> edgeBridge = new UnconditionalJumpEdge<>(
                newEntry,
                next
        );
        newEntry.add(new UnconditionalJumpStmt(next, edgeBridge));
        cfg.addEdge(edgeBridge);

        initBlock.setFlag(SkidBlock.FLAG_NO_OPAQUE, true);


        cfg.getAllChildren(initBlock).forEach(vertex -> {
            if (IntegerBlockPredicateRenderer.DEBUG) {
                final Local debugLocal = cfg.getLocals().get(cfg.getLocals().getMaxLocals() + 2);
                vertex.add(
                        0,
                        new SkidCopyVarStmt(
                                new VarExpr(debugLocal, Type.getType(String.class)),
                                new ConstantExpr(
                                        "<-> Opaque protected <->",
                                        TypeUtil.STRING_TYPE
                                )
                        )
                );

                vertex.add(
                        new SkidCopyVarStmt(
                                new VarExpr(debugLocal, Type.getType(String.class)),
                                new ConstantExpr(
                                        "<-> Opaque Unprotected <->",
                                        TypeUtil.STRING_TYPE
                                )
                        )
                );
            }

            vertex.setFlag(SkidBlock.FLAG_NO_OPAQUE, true);
        });

        if (IntegerBlockPredicateRenderer.DEBUG) {
            final Local debugLocal = cfg.getLocals().get(cfg.getLocals().getMaxLocals() + 2);
            initBlock.add(
                    0,
                    new SkidCopyVarStmt(
                            new VarExpr(debugLocal, Type.getType(String.class)),
                            new ConstantExpr(
                                    "<-> Opaque protected [INIT START] <->",
                                    TypeUtil.STRING_TYPE
                            )
                    )
            );

            initBlock.add(
                    new SkidCopyVarStmt(
                            new VarExpr(debugLocal, Type.getType(String.class)),
                            new ConstantExpr(
                                    "<-> Opaque Unprotected [INIT END] <->",
                                    TypeUtil.STRING_TYPE
                            )
                    )
            );
        }

        //System.out.println("Selected" + CFGUtils.printBlock(initBlock));

        if (IntegerBlockPredicateRenderer.DEBUG) {
            final Local debugLocal = cfg.getLocals().get(cfg.getLocals().getMaxLocals() + 2);
            newEntry.add(
                    0,
                    new SkidCopyVarStmt(
                            new VarExpr(debugLocal, Type.getType(String.class)),
                            new ConstantExpr(
                                    "<-> Opaque Unprotected [START] <->",
                                    TypeUtil.STRING_TYPE
                            )
                    )
            );
            newEntry.add(
                    new SkidCopyVarStmt(
                            new VarExpr(debugLocal, Type.getType(String.class)),
                            new ConstantExpr(
                                    "<-> Opaque Unprotected [END] <->",
                                    TypeUtil.STRING_TYPE
                            )
                    )
            );

        }

        return (this.initBlock = newEntry);
    }

    public boolean isExtruded() {
        return extruded;
    }

    public void setExtruded(boolean extruded) {
        this.extruded = extruded;
    }

    public boolean isExempt(final MethodExempt... exemptions) {
        return MethodExempt.isExempt(this, exemptions);
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

    @Deprecated
    public List<SkidInvocation> getInvocations() {
        return invokers;
    }

    public SkidClassNode getParent() {
        return (SkidClassNode) owner;
    }
}
