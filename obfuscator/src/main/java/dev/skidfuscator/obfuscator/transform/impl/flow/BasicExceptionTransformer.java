package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpEdge;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class BasicExceptionTransformer extends AbstractTransformer {
    public BasicExceptionTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public BasicExceptionTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"Basic Exception Transformer", children);
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        if (methodNode.isAbstract() || methodNode.isInit())
            return;

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        for (BasicBlock entry : new HashSet<>(cfg.vertices())) {
            if (entry.size() == 0)
                continue;

            // Todo add hashing to amplify difficulty and remove key exposure
            // Todo make this a better system
            final int seed = methodNode.getBlockPredicate((SkidBlock) entry);

            // Create hash
            final SkiddedHash hash = NumberManager.randomHasher()
                    .hash(seed, cfg, methodNode.getFlowPredicate().getGetter());
            final ConstantExpr var_const = new ConstantExpr(hash.getHash());

            // Todo add more boilerplates + add exception rotation
            final BasicBlock fuckup = Blocks.exception(cfg);

            // Todo change blocks to be skiddedblocks to add method to directly add these
            final FakeConditionalJumpStmt jump_stmt = new FakeConditionalJumpStmt(hash.getExpr(), var_const, fuckup, ConditionalJumpStmt.ComparisonType.NE);
            final FakeConditionalJumpEdge<BasicBlock> jump_edge = new FakeConditionalJumpEdge<>(entry, fuckup, Opcodes.IF_ICMPNE);

            if (entry.get(entry.size() - 1) instanceof UnconditionalJumpStmt)
                entry.add(entry.size() - 1, jump_stmt);
            else
                entry.add(jump_stmt);
            cfg.addEdge(jump_edge);

            event.tick();

            // Regular debug
                /*
                final Local local1 = entry.cfg.getLocals().get(entry.cfg.getLocals().getMaxLocals() + 2);
                entry.add(new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                        new ConstantExpr(entry.getDisplayName() +" : var expect: " + var_const.getConstant())));
                */
        }
    }
}
