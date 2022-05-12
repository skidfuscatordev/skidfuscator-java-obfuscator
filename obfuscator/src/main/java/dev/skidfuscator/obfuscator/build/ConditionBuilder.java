package dev.skidfuscator.obfuscator.build;

import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.SSAFactory;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;

import java.util.Stack;

public class ConditionBuilder {
    private final SSAFactory factory;
    private final ControlFlowGraph cfg;
    private BasicBlock current;
    private final BasicBlock immediate;
    private final Stack<Expr> stack;

    public ConditionBuilder(SSAFactory factory, ControlFlowGraph cfg, BasicBlock current, Stack<Expr> stack) {
        this.factory = factory;
        this.cfg = cfg;
        this.current = current;
        this.immediate = cfg.getImmediate(current);
        cfg.removeEdge(cfg.getImmediateEdge(current));
        this.stack = stack;
    }

    public ConditionBuilder ifEqual(final BasicBlock target) {
        return condition(target, ConditionalJumpStmt.ComparisonType.EQ);
    }

    public ConditionBuilder ifNotEqual(final BasicBlock target) {
        return condition(target, ConditionalJumpStmt.ComparisonType.NE);
    }

    public ConditionBuilder ifGreater(final BasicBlock target) {
        return condition(target, ConditionalJumpStmt.ComparisonType.GT);
    }

    public ConditionBuilder ifLower(final BasicBlock target) {
        return condition(target, ConditionalJumpStmt.ComparisonType.LT);
    }

    public ConditionBuilder ifGreaterOrEqual(final BasicBlock target) {
        return condition(target, ConditionalJumpStmt.ComparisonType.GE);
    }

    public ConditionBuilder ifLowerOrEqual(final BasicBlock target) {
        return condition(target, ConditionalJumpStmt.ComparisonType.LE);
    }

    public BlockBuilder exit() {
        // Push the new block
        final UnconditionalJumpEdge<BasicBlock> moveEdge = new UnconditionalJumpEdge<>(
                current,
                immediate
        );
        final UnconditionalJumpStmt moveStack = new UnconditionalJumpStmt(immediate, moveEdge);

        current.add(moveStack);
        cfg.addEdge(moveEdge);

        return new BlockBuilder(factory, cfg, stack);
    }

    public ConditionBuilder condition(final BasicBlock target, final ConditionalJumpStmt.ComparisonType type) {
        // Todo Make this support multiple condition by using a local
        //      also improve local system to support many uses without
        //      spamming them af
        Expr right = stack.pop();
        Expr left = stack.pop();

        /*
         * Create a conditional jump and add it to
         * the block, then push it to the control
         * flow graph.
         */
        final ConditionalJumpStmt jumpStmt = factory
                .conditional_jump_stmt()
                .right(right)
                .left(left)
                .type(type)
                .target(target)
                .build();

        // Not necessary to handle
        final ConditionalJumpEdge<BasicBlock> jumpEdge = new ConditionalJumpEdge<>(
                current,
                target,
                0 // Only used for debug reasons
        );

        current.add(jumpStmt);
        cfg.addEdge(jumpEdge);

        next();

        return this;
    }

    private void next() {
        /*
         * Create a new immediate block, clear the immediate
         * range and allow the block to be used for further
         * jumps and stuff
         */
        final BasicBlock old = current;
        current = new BasicBlock(cfg);
        cfg.addVertex(current);

        // Push the new block
        final UnconditionalJumpEdge<BasicBlock> moveEdge = new UnconditionalJumpEdge<>(
                old,
                current
        );
        final UnconditionalJumpStmt moveStack = new UnconditionalJumpStmt(current, moveEdge);

        old.add(moveStack);
        cfg.addEdge(moveEdge);
    }
}
