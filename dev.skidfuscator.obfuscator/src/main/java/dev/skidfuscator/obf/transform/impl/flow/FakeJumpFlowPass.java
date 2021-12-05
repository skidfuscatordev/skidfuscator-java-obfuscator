package dev.skidfuscator.obf.transform.impl.flow;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.maple.FakeConditionalJumpStmt;
import dev.skidfuscator.obf.number.NumberManager;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import dev.skidfuscator.obf.utils.Blocks;
import dev.skidfuscator.obf.utils.RandomUtil;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;

import java.util.HashSet;

public class FakeJumpFlowPass implements FlowPass {

    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract() || methodNode.isInit())
                continue;

            if (methodNode.getNode().node.instructions.size() > 10000)
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;


            for (BasicBlock entry : new HashSet<>(cfg.vertices())) {
                if (entry.size() == 0 || entry.getStack() != null && !entry.getStack().isEmpty())
                    continue;

                // Todo add hashing to amplify difficulty and remove key exposure
                // Todo make this a better system
                final int seed = methodNode.getBlock(entry).getSeed();

                // Create hash
                final SkiddedHash hash = NumberManager.hash(seed, methodNode.getLocal());
                final ConstantExpr var_const = new ConstantExpr(hash.getHash());

                // Todo add more boilerplates + add exception rotation
                final BasicBlock fuckup = entry;

                // Todo change blocks to be skiddedblocks to add method to directly add these
                final FakeConditionalJumpStmt jump_stmt = new FakeConditionalJumpStmt(hash.getExpr(), var_const, fuckup, ConditionalJumpStmt.ComparisonType.NE);
                final ConditionalJumpEdge<BasicBlock> jump_edge = new ConditionalJumpEdge<>(entry, fuckup, jump_stmt.getOpcode());

                if (entry.get(entry.size() - 1) instanceof UnconditionalJumpStmt)
                    entry.add(entry.size() - 1, jump_stmt);
                else
                    entry.add(jump_stmt);

                cfg.addEdge(jump_edge);

                session.count();

                // Regular debug
                /*
                final Local local1 = entry.cfg.getLocals().get(entry.cfg.getLocals().getMaxLocals() + 2);
                entry.add(new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                        new ConstantExpr(entry.getDisplayName() +" : var expect: " + var_const.getConstant())));
                */
            }
        }
    }

    @Override
    public String getName() {
        return "Fake Jump";
    }
}
