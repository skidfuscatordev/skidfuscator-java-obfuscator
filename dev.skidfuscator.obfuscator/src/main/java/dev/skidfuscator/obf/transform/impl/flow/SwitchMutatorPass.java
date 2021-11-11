package dev.skidfuscator.obf.transform.impl.flow;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.maple.FakeArithmeticExpr;
import dev.skidfuscator.obf.number.NumberManager;
import dev.skidfuscator.obf.skidasm.SkidBlock;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import org.mapleir.flowgraph.edges.SwitchEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class SwitchMutatorPass implements FlowPass {
    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract() || methodNode.isInit())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

            for (BasicBlock vertex : cfg.vertices()) {
                for (Stmt stmt : new HashSet<>(vertex)) {
                    if (!(stmt instanceof SwitchStmt)) {
                        continue;
                    }

                    final SwitchStmt switchStmt = (SwitchStmt) stmt;
                    final SkidBlock skidBlock = methodNode.getBlock(vertex);
                    final Expr switchExpr = switchStmt.getExpression();
                    switchExpr.unlink();
                    final Expr expr = new FakeArithmeticExpr(
                            switchExpr,
                            new VarExpr(methodNode.getLocal(), Type.INT_TYPE),
                            ArithmeticExpr.Operator.XOR
                    );

                    switchStmt.setExpression(expr);

                    final Set<Map.Entry<Integer, BasicBlock>> entrySet = new HashSet<>(switchStmt.getTargets().entrySet());

                    final BiMap<Integer, Integer> clearingMap = HashBiMap.create();
                    final List<Integer> toSort = new ArrayList<>();

                    for (Map.Entry<Integer, BasicBlock> entry : entrySet) {
                        final int encrypted = entry.getKey() ^ skidBlock.getSeed();
                        final int key = entry.getKey();

                        clearingMap.put(encrypted, key);
                        toSort.add(encrypted);
                        //switchStmt.getTargets().put(entry.getKey() ^ skidBlock.getSeed(), entry.getValue());
                    }

                    final LinkedHashMap<Integer, BasicBlock> hashedMap = new LinkedHashMap<>();

                    Collections.sort(toSort);

                    for (Integer hashed : toSort) {
                        final Integer var = clearingMap.get(hashed);
                        final BasicBlock block = switchStmt.getTargets().get(var);

                        hashedMap.put(hashed, block);
                    }

                    switchStmt.getTargets().clear();
                    switchStmt.setTargets(hashedMap);

                    final List<SwitchEdge<BasicBlock>> switchEdges = cfg.getEdges(vertex).stream()
                                    .filter(e -> e instanceof SwitchEdge)
                                    .map(e -> (SwitchEdge<BasicBlock>) e)
                                    .filter(e -> {
                                        final BasicBlock block = hashedMap.get(e.value);

                                        if (block == null)
                                            return false;

                                        return block == e.dst();
                                    }).collect(Collectors.toList());

                    for (SwitchEdge<BasicBlock> switchEdge : switchEdges) {
                        cfg.removeEdge(switchEdge);
                    }

                    hashedMap.forEach((var, dst) -> {
                        cfg.addEdge(new SwitchEdge<>(vertex, dst, var));
                    });

                    session.count();
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Switch Mutator";
    }
}
