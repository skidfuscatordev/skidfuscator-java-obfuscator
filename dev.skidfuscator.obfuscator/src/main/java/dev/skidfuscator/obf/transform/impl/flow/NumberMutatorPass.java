package dev.skidfuscator.obf.transform.impl.flow;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.number.NumberManager;
import dev.skidfuscator.obf.skidasm.SkidBlock;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class NumberMutatorPass implements FlowPass {
    private static final Set<Type> types = new HashSet<>(Arrays.asList(
            Type.INT_TYPE,
            Type.SHORT_TYPE,
            Type.BYTE_TYPE
    ));

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

            for (CodeUnit codeUnit : cfg.allExprStream().collect(Collectors.toList())) {
                if (!(codeUnit instanceof Expr))
                    continue;

                final Expr expr = (Expr) codeUnit;
                if (!(expr instanceof ConstantExpr))
                    continue;

                final ConstantExpr constantExpr = (ConstantExpr) expr;
                if (!types.contains(constantExpr.getType()))
                    continue;

                final CodeUnit parent = expr.getParent();
                final int index = parent.indexOf(constantExpr);

                final SkidBlock skidBlock = methodNode.getBlock(codeUnit.getBlock());

                final Expr modified = NumberManager.encrypt(((Number) constantExpr.getConstant()).intValue(), skidBlock.getSeed(), methodNode.getLocal());
                parent.writeAt(modified, index);
            }
        }

    }

    @Override
    public String getName() {
        return null;
    }
}
