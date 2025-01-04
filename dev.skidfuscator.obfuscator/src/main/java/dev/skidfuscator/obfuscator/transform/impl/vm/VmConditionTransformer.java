package dev.skidfuscator.obfuscator.transform.impl.vm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventPriority;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.FinalMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.*;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.ArrayStoreStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Set;

public class VmConditionTransformer extends AbstractTransformer {
    public VmConditionTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "VM Transformer");
    }

    @Listen(EventPriority.HIGHEST)
    void handle(final InitMethodTransformEvent event) {
        final ControlFlowGraph cfg = event.getMethodNode().getCfg();
        if (cfg == null) {
            return;
        }

        //if (!event.getMethodNode().getName().equalsIgnoreCase("run")
        //|| !event.getMethodNode().getParent().getName().contains("AnnotationTest"))
        //    return;

        int highestItem = cfg.getMethodNode().isStatic() ? 0 : 1;
        for (Type argumentType : Type.getArgumentTypes(cfg.getMethodNode().getDesc())) {
            highestItem += argumentType.getSize();
        }

        final int highestIndex = highestItem;
        final int maxIndex = cfg.getLocals().getMaxLocals() + 1;

        final Local objectArrayLocal = cfg.getLocals().nextFree();
        final Local intArrayLocal = cfg.getLocals().nextFree();
        final Local doubleArrayLocal = cfg.getLocals().nextFree();
        final Local longArrayLocal = cfg.getLocals().nextFree();
        final Local floatArrayLocal = cfg.getLocals().nextFree();

        final Set<Local> locals = Set.of(
                objectArrayLocal,
                intArrayLocal,
                doubleArrayLocal,
                longArrayLocal,
                floatArrayLocal
        );

        // We need to work on a copy since we'll be modifying the blocks
        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            for (Stmt stmt : new HashSet<>(block)) {
                copy:
                {
                    if (stmt instanceof CopyVarStmt copyVarStmt) {
                        // [precondition] skip all parameters
                        // todo: make internal method
                        if (copyVarStmt.getVariable().getLocal().getIndex() < highestItem)
                            break copy;

                        if (copyVarStmt.getExpression() instanceof AllocObjectExpr)
                            break copy;

                        Local local;
                        Type type;
                        TypeUtils.ArrayType arrayType;

                        switch (copyVarStmt.getType().getSort()) {
                            case Type.LONG -> {
                                local = longArrayLocal;
                                type = TypeUtils.LONG_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.LONG;
                            }
                            case Type.DOUBLE -> {
                                local = doubleArrayLocal;
                                type = TypeUtils.DOUBLE_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.DOUBLE;
                            }
                            case Type.FLOAT -> {
                                local = floatArrayLocal;
                                type = TypeUtils.FLOAT_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.FLOAT;
                            }
                            case Type.INT, Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT -> {
                                local = intArrayLocal;
                                type = TypeUtils.INT_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.INT;
                            }
                            case Type.OBJECT, Type.ARRAY -> {
                                local = objectArrayLocal;
                                type = TypeUtils.OBJECT_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.OBJECT;
                            }
                            default -> throw new IllegalStateException("Failed");
                        }

                        final ArrayStoreStmt replace = new VmArrayStoreStmt(
                                new VarExpr(local, type),
                                new ConstantExpr(copyVarStmt.getVariable().getIndex(), Type.INT_TYPE),
                                copyVarStmt.getExpression().copy(),
                                arrayType
                        );
                        block.replace(stmt, replace);

                        stmt = replace;
                        //System.out.println(String.format(
                        //        "Replaced old %s with %s", stmt, replace
                        //));
                    }
                }
            }
        }

        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            for (Stmt stmt : new HashSet<>(block)) {
                for (CodeUnit codeUnit : new HashSet<>(stmt.traverse())) {
                    if (codeUnit instanceof InvocationExpr invocationExpr) {
                        if (!invocationExpr.getName().equalsIgnoreCase("<init>"))
                            continue;

                        final Stmt rootParent = invocationExpr.getRootParent();

                        // [precondition] initialisation must be forwarded
                        if (!(invocationExpr.getArgumentExprs()[0] instanceof VarExpr))
                            continue;

                        final VarExpr varExpr = (VarExpr) invocationExpr.getArgumentExprs()[0];
                        rootParent.getBlock().add(
                                rootParent.getBlock().indexOf(rootParent) + 1,
                                new ArrayStoreStmt(
                                        new VarExpr(objectArrayLocal, TypeUtils.OBJECT_ARRAY_TYPE),
                                        new ConstantExpr(varExpr.getIndex(), Type.INT_TYPE),
                                        varExpr.copy(),
                                        TypeUtils.arrayTypeOfObject(varExpr.getType())
                                )
                        );
                    }
                    if (codeUnit instanceof VarExpr varExpr) {
                        final CodeUnit parent = varExpr.getParent();
                        //System.out.println(String.format(
                        //        "Printing out %s (parent: %s)", codeUnit, parent
                        ///));

                        if (varExpr.getLocal().getIndex() < highestIndex) {
                            //System.out.println("=> Skipping because of index " + varExpr.getLocal().getIndex() + " (max: " + highestIndex + ")");
                            continue;
                        }

                        if (locals.contains(varExpr.getLocal())) {
                            //System.out.println("=> Skipping because protected");
                            continue;
                        }

                        if (parent instanceof InvocationExpr invocationExpr
                                && invocationExpr.getName().equalsIgnoreCase("<init>")
                                && varExpr.getType().getSort() == Type.OBJECT) {
                            if (varExpr.getType().equals(Type.getType("L" + invocationExpr.getOwner() + ";"))) {
                                //System.out.println(String.format(
                                //        "Skipping suspicious init call %s", varExpr.getParent()
                                //));
                                continue;
                            }
                        }

                        Local local;
                        Type type;
                        TypeUtils.ArrayType arrayType;
                        switch (varExpr.getType().getSort()) {
                            case Type.LONG -> {
                                local = longArrayLocal;
                                type = TypeUtils.LONG_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.LONG;
                            }
                            case Type.DOUBLE -> {
                                local = doubleArrayLocal;
                                type = TypeUtils.DOUBLE_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.DOUBLE;
                            }
                            case Type.FLOAT -> {
                                local = floatArrayLocal;
                                type = TypeUtils.FLOAT_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.FLOAT;
                            }
                            case Type.INT, Type.BOOLEAN, Type.SHORT, Type.BYTE, Type.CHAR -> {
                                local = intArrayLocal;
                                type = TypeUtils.INT_ARRAY_TYPE;
                                arrayType = TypeUtils.ArrayType.INT;
                            }
                            case Type.OBJECT, Type.ARRAY -> {
                                local = objectArrayLocal;
                                type = TypeUtils.OBJECT_ARRAY_TYPE;
                                arrayType = TypeUtils.arrayTypeOfObject(varExpr.getType());
                            }
                            default -> throw new IllegalStateException("Failed");
                        }

                        if (varExpr.getParent() == null)
                            throw new IllegalStateException(
                                    "Hanging varexpr: " + varExpr
                            );

                        Expr newExpr = new ArrayLoadExpr(
                                new VarExpr(local, type),
                                new ConstantExpr(varExpr.getIndex(), Type.INT_TYPE),
                                arrayType
                        );

                        if (type == TypeUtils.OBJECT_ARRAY_TYPE) {
                            Type computedType = varExpr.getLocal().getType() == null ? varExpr.getType() : varExpr.getLocal().getType();

                            if (parent instanceof InvocationExpr invocationExpr) {
                                final int argumentIndex = invocationExpr.indexOf(varExpr);

                                if (argumentIndex < 0) {
                                    throw new IllegalStateException(
                                            String.format("Cannot be null argument: %s for %s", varExpr, parent)
                                    );
                                }
                                final boolean isAddedArg = invocationExpr instanceof VirtualInvocationExpr;
                                computedType = argumentIndex == 0 && isAddedArg
                                        ? Type.getType(String.format("L%s;", invocationExpr.getOwner()))
                                        : Type.getArgumentTypes(invocationExpr.getDesc())[argumentIndex - (isAddedArg ? 1 : 0)];
                            }
                            if (!computedType.equals(TypeUtils.OBJECT_TYPE)) {
                                newExpr = new CastExpr(
                                        newExpr,
                                        computedType
                                );
                            }
                        }

                        parent.overwrite(varExpr, newExpr);
                    }
                }
            }
        }

        final CopyVarStmt floatArray = new CopyVarStmt(
                new VarExpr(floatArrayLocal, Type.getType("[F;")),
                new NewArrayExpr(new Expr[]{
                        new ConstantExpr(maxIndex, Type.INT_TYPE)
                }, TypeUtils.FLOAT_ARRAY_TYPE)
        );
        final CopyVarStmt longArray = new CopyVarStmt(
                new VarExpr(longArrayLocal, Type.getType("[J;")),
                new NewArrayExpr(new Expr[]{
                        new ConstantExpr(maxIndex, Type.INT_TYPE)
                }, TypeUtils.LONG_ARRAY_TYPE)
        );

        final CopyVarStmt doubleArray = new CopyVarStmt(
                new VarExpr(doubleArrayLocal, Type.getType("[D;")),
                new NewArrayExpr(new Expr[]{
                        new ConstantExpr(maxIndex, Type.INT_TYPE)
                }, TypeUtils.DOUBLE_ARRAY_TYPE)
        );
        final CopyVarStmt objectArray = new CopyVarStmt(
                new VarExpr(objectArrayLocal, Type.getType("[Ljava/lang/Object;")),
                new NewArrayExpr(new Expr[]{
                        new ConstantExpr(maxIndex, Type.INT_TYPE)
                }, TypeUtils.OBJECT_ARRAY_TYPE)
        );
        final CopyVarStmt intArray = new CopyVarStmt(
                new VarExpr(intArrayLocal, Type.getType("[I;")),
                new NewArrayExpr(new Expr[]{
                        new ConstantExpr(maxIndex, Type.INT_TYPE)
                }, TypeUtils.INT_ARRAY_TYPE)
        );

        System.out.println(cfg.toString());

        cfg.getEntries().iterator().next().add(0, objectArray);
        cfg.getEntries().iterator().next().add(0, intArray);
        cfg.getEntries().iterator().next().add(0, floatArray);
        cfg.getEntries().iterator().next().add(0, longArray);
        cfg.getEntries().iterator().next().add(0, doubleArray);
    }
} 