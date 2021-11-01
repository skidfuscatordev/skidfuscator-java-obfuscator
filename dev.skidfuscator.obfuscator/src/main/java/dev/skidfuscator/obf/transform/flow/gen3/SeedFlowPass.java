package dev.skidfuscator.obf.transform.flow.gen3;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform.flow.FlowPass;
import dev.skidfuscator.obf.transform.yggdrasil.SkidMethod;
import dev.skidfuscator.obf.utils.Blocks;
import dev.skidfuscator.obf.utils.RandomUtil;
import org.mapleir.X;
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.ImmediateEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SeedFlowPass implements FlowPass {
    private final Map<BasicBlock, SeededBlock> blocks = new HashMap<>();

    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract())
                continue;

            //run(session.getCxt().getIRCache().get(methodNode));
        }
    }
}
