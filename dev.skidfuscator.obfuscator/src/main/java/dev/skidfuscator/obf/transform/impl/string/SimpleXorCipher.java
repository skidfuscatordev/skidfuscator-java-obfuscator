package dev.skidfuscator.obf.transform.impl.string;

import dev.skidfuscator.obf.attribute.AttributeKey;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidBlock;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import dev.skidfuscator.obf.transform.impl.flow.FlowPass;
import dev.skidfuscator.obf.utils.MiscUtils;
import dev.skidfuscator.obf.utils.generate.XorEncryption;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xor cipher, a cipher so bad it hurts my eyes out because a simple bruteforce attack could
 * reverse engineer this...
 *
 * Buuuuuuut....
 *
 * Yes
 *
 * Kappa
 *
 * The basic concept of the cipher is we take the digit value of a character, xor it, and tada.
 * Yep. Tada.
 *
 * Yes.
 */
public class SimpleXorCipher implements FlowPass {
    private static final Type STRING_TYPE = Type.getType(String.class);

    private static final Set<ClassNode> INJECTED = new HashSet<>();

    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract() || methodNode.isInit())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

            final List<ConstantExpr> stringList = cfg.allExprStream()
                    .filter(e -> e instanceof ConstantExpr)
                    .map(e -> (ConstantExpr) e)
                    .filter(e -> e.getConstant() instanceof String)
                    .collect(Collectors.toList());

            if (stringList.isEmpty())
                continue;

            final ClassNode parent = methodNode.getNode().owner;

            if (!INJECTED.contains(parent)) {
                XorEncryption.visit(parent.node);
                INJECTED.add(parent);
            }

            for (ConstantExpr constantExpr : stringList) {
                final CodeUnit parentExpr = constantExpr.getParent();

                if (parentExpr instanceof DynamicInvocationExpr || parentExpr instanceof InitialisedObjectExpr)
                    continue;

                final BasicBlock block = constantExpr.getBlock();
                final SkidBlock skidBlock = methodNode.getBlock(block);
                final Local local = methodNode.getLocal();

                final String encrypted = XorEncryption.factor((String) constantExpr.getConstant(), skidBlock.getSeed());
                final Expr constant_load = new ConstantExpr(encrypted, STRING_TYPE);
                final Expr local_load = new VarExpr(local, Type.INT_TYPE);

                final Expr invocation = new StaticInvocationExpr(
                        new Expr[]{constant_load, local_load},
                        parent.node.name,
                        XorEncryption.NAME,
                        "(Ljava/lang/String;I)Ljava/lang/String;"
                );

                session.count();

                if (parentExpr instanceof InvocationExpr) {
                    final InvocationExpr invocationExpr = ((InvocationExpr) parentExpr);
                    final int index = MiscUtils.indexOf(invocationExpr.getArgumentExprs(), constantExpr);
                    //constantExpr.unlink();
                    invocationExpr.getArgumentExprs()[index] = invocation;
                } else {
                    final int index = parentExpr.indexOf(constantExpr);
                    parentExpr.writeAt(invocation, index);
                }


            }


        }
    }

    @Override
    public String getName() {
        return "Xor String Encryption";
    }
}
