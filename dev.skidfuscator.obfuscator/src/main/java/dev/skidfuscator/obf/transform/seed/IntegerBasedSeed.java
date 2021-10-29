package dev.skidfuscator.obf.transform.seed;

import dev.skidfuscator.obf.transform.caller.CallerType;
import dev.skidfuscator.obf.transform.yggdrasil.SkidInvocation;
import dev.skidfuscator.obf.transform.yggdrasil.SkidMethod;
import dev.skidfuscator.obf.transform_legacy.number.NumberManager;
import dev.skidfuscator.obf.transform_legacy.parameter.Parameter;
import dev.skidfuscator.obf.utils.OpcodeUtil;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.impl.BasicLocal;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

import java.util.*;

public class IntegerBasedSeed extends AbstractSeed<Integer> {
    public IntegerBasedSeed(SkidMethod parent, Integer privateSeed, Integer publicSeed) {
        super(parent, privateSeed, publicSeed);
    }

    private Local local;

    @Override
    public void renderPrivate(final MethodNode methodNode, final ControlFlowGraph cfg) {
        final boolean free = parent.getCallerType() == CallerType.APPLICATION;

        if (free && false) {
            final BasicLocal local = cfg.getLocals().getNextFreeLocal(false);

            int stackHeight = OpcodeUtil.getArgumentsSizes(this.parent.getParameter().getDesc());
            if (!this.parent.isStatic()) stackHeight += 1;

            final Map<String, Local> localMap = new HashMap<>();

            for (Map.Entry<String, Local> stringLocalEntry : cfg.getLocals().getCache().entrySet()) {
                final String old = stringLocalEntry.getKey();
                final String oldStringId = old.split("var")[1].split("_")[0];
                final int oldId = Integer.parseInt(oldStringId);

                if (oldId < stackHeight) {
                    localMap.put(old, stringLocalEntry.getValue());
                    continue;
                }
                final int newId = oldId + 1;

                final String newVar = old.replace(oldStringId, Integer.toString(newId));
                stringLocalEntry.getValue().setIndex(stringLocalEntry.getValue().getIndex() + 1);
                localMap.put(newVar, stringLocalEntry.getValue());
            }

            cfg.getLocals().getCache().clear();
            cfg.getLocals().getCache().putAll(localMap);

            /*
             * We will always place the long local as the final one, hence we load the last parameter
             */
            final VarExpr seed = new VarExpr(cfg.getLocals().get(stackHeight, false), Type.INT_TYPE);

            /*
             * We then modify through arbitrary bitwise operations the public seed to the private seed
             */
            final Expr seedExpr = NumberManager.transform(privateSeed, publicSeed, seed);

            /*
             * We create a variable to store it then proceed to store it
             */
            final VarExpr privateSeedLoader = new VarExpr(local, Type.INT_TYPE);
            final CopyVarStmt privateSeedSetter = new CopyVarStmt(privateSeedLoader, seedExpr);
            cfg.verticesInOrder().get(0).add(privateSeedSetter);

            this.local = local;
        }

        else {
            final BasicLocal local =/* cfg.getLocals().get(*/cfg.getLocals().get(cfg.getLocals().getMaxLocals() + 2)/*.getIndex(), false)*/;

            /*
             * Here we initialize the private seed as a root factor. This is the sensitive part of the application
             * we'll have to rework it to add some protection
             */
            final ConstantExpr privateSeed = new ConstantExpr(this.privateSeed, Type.INT_TYPE);
            final VarExpr privateSeedLoader = new VarExpr(local, Type.INT_TYPE);
            final CopyVarStmt privateSeedSetter = new CopyVarStmt(privateSeedLoader, privateSeed);
            cfg.verticesInOrder().get(0).add(0, privateSeedSetter);

            this.local = local;
        }
    }

    @Override
    public void renderPublic(List<MethodNode> methodNodes) {
        final boolean free = parent.getCallerType() == CallerType.APPLICATION;

        if (!free || true)
            return;

        parent.getParameter().addParameter(Type.INT_TYPE);
        for (MethodNode methodNode : methodNodes) {
            if (methodNode.node.localVariables == null)
                methodNode.node.localVariables = new ArrayList<>();

            for (LocalVariableNode localVariable : methodNode.node.localVariables) {
                if (localVariable.index >= local.getIndex()) {
                    localVariable.index++;
                }
            }

            methodNode.node.localVariables.add(new LocalVariableNode(
                    "hash",
                    Type.INT_TYPE.getDescriptor(),
                    "",
                    new LabelNode(),
                    new LabelNode(),
                    local.getIndex()
            ));
        }

        final String desc = parent.getParameter().getDesc();

        for (SkidInvocation expr : parent.getInvocationModal()) {
            final List<Expr> params = new ArrayList<>(Arrays.asList(expr.getInvocationExpr().getArgumentExprs()));
            params.add(new ConstantExpr(
                    this.publicSeed,
                    Type.INT_TYPE
            ));

            final Expr[] exprs = new Expr[params.size()];

            for (int i = 0; i < params.size(); i++) {
                exprs[i] = params.get(i);
            }
            
            expr.getInvocationExpr().setDesc(desc);
            expr.getInvocationExpr().setArgumentExprs(exprs);
        }
    }

    @Override
    public Integer getPublic() {
        return this.publicSeed;
    }

    @Override
    public Integer getPrivate() {
        return privateSeed;
    }

    @Override
    public Expr getPrivateLoader() {
        return new VarExpr(local, Type.INT_TYPE);
    }
}
