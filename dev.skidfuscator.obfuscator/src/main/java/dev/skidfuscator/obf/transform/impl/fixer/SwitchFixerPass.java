package dev.skidfuscator.obf.transform.impl.fixer;

import com.google.common.collect.Lists;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import dev.skidfuscator.obf.transform.impl.flow.FlowPass;
import org.mapleir.asm.ClassNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.DefaultSwitchEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.objectweb.asm.Type;

import java.util.*;

public class SwitchFixerPass implements FlowPass {
    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

            for (BasicBlock vertex : new HashSet<>(cfg.vertices())) {
                for (Stmt stmt : vertex) {
                    if (!(stmt instanceof SwitchStmt))
                        continue;
                    
                    final SwitchStmt switchStmt = (SwitchStmt) stmt;
                    final BasicBlock block = new BasicBlock(cfg);
                    block.add(new UnconditionalJumpStmt(switchStmt.getDefaultTarget()));
                    cfg.addEdge(new UnconditionalJumpEdge<>(block, switchStmt.getDefaultTarget()));

                    new HashSet<>(cfg.getEdges(vertex)).stream()
                            .filter(e -> e instanceof DefaultSwitchEdge)
                            .map(e -> (DefaultSwitchEdge<BasicBlock>) e)
                            .filter(e -> e.dst() == switchStmt.getDefaultTarget())
                            .findAny()
                            .ifPresent(cfg::removeEdge);

                    cfg.addVertex(block);
                    cfg.addEdge(new DefaultSwitchEdge<>(vertex, block));
                    switchStmt.setDefaultTarget(block);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Switch Fixer";
    }
}
