package dev.skidfuscator.obf.yggdrasil.method.hash;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

/**
 * @author Ghast
 * @since 08/03/2021
 * SkidfuscatorV2 © 2021
 */

@Data
@EqualsAndHashCode
public class InvokerHash {
    private final MethodNode methodNode;
    private final InvocationExpr stmt;

    public InvokerHash(MethodNode methodNode, InvocationExpr stmt) {
        this.methodNode = methodNode;
        this.stmt = stmt;
    }
}
