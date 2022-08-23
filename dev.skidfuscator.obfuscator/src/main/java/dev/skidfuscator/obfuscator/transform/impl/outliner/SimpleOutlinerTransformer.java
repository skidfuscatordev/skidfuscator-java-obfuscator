package dev.skidfuscator.obfuscator.transform.impl.outliner;

import com.google.common.collect.Streams;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.FinalMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleOutlinerTransformer extends AbstractTransformer {
    public SimpleOutlinerTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Outliner");
    }

    @Listen
    void handle(final FinalMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        final SkidControlFlowGraph cfg = methodNode.getCfg();

        if (methodNode.isInit())
            return;

        for (BasicBlock vertex : cfg.vertices()) {
            final Set<Stmt> extrude = new HashSet<>();
            final Set<VarExpr> passThrough = new HashSet<>();

            for (Stmt stmt : new HashSet<>(vertex)) {
                if (!(stmt instanceof PopStmt)) {
                    if (extrude.isEmpty())
                        continue;

                    // If no longer a pop, extrude the statements to new method
                    final ExtrusionResult result = this.extrude(methodNode, extrude, passThrough);
                    final List<Expr> exprs = result.getParams().stream().map(e -> new VarExpr(
                            e,
                            result.getType(e)
                    )).collect(Collectors.toList());

                    if (!methodNode.isStatic()) {
                        exprs.add(0, new VarExpr(cfg.getSelfLocal(), methodNode.getOwnerType()));
                    }

                    vertex.add(vertex.indexOf(stmt), new PopStmt(methodNode.isStatic()
                            ? new StaticInvocationExpr(
                                    exprs.toArray(new Expr[0]),
                                    result.getMethodNode().getOwner(),
                                    result.getMethodNode().getName(),
                                    result.getMethodNode().getDesc()
                            )
                            : new VirtualInvocationExpr(
                                    InvocationExpr.CallType.VIRTUAL,
                                    exprs.toArray(new Expr[0]),
                                    result.getMethodNode().getOwner(),
                                    result.getMethodNode().getName(),
                                    result.getMethodNode().getDesc()
                            )
                    ));

                    extrude.clear();
                    passThrough.clear();
                    continue;
                }

                final PopStmt popStmt = (PopStmt) stmt;

                if (popStmt.getExpression() instanceof VirtualInvocationExpr) {
                    final VirtualInvocationExpr expr = (VirtualInvocationExpr) popStmt.getExpression();
                    if (expr.getName().equals("<init>"))
                        continue;
                }

                // Add statement to list
                extrude.add(stmt);

                // Add all variable calls to list
                Streams.stream(stmt.enumerateOnlyChildren())
                        .filter(VarExpr.class::isInstance)
                        .map(VarExpr.class::cast)
                        .forEach(passThrough::add);
            }

            if (extrude.size() > 0) {
                this.extrude(methodNode, extrude, passThrough);
            }
        }
    }

    private ExtrusionResult extrude(final SkidMethodNode methodNode, final Set<Stmt> stmts, final Set<VarExpr> calls) {
        final Parameter parameter = new Parameter("()V");

        final Map<Local, Set<VarExpr>> callmap = new HashMap<>();
        final Set<VarExpr> cleaned = new HashSet<>();

        loop: for (VarExpr call : calls) {
            Set<VarExpr> exprs = callmap.computeIfAbsent(call.getLocal(), e -> new HashSet<>());

            for (VarExpr expr : exprs) {
                if (call.equivalent(expr))
                    continue loop;
            }

            exprs.add(call);
            cleaned.add(call);
        }

        int header = methodNode.isStatic() ? 0 : 1;

        final Map<Local, Integer> localToNewMethodLocalId = new HashMap<>();
        final List<Local> params = new ArrayList<>();
        final Map<Local, Type> types = new HashMap<>();
        for (VarExpr varExpr : cleaned) {
            // Compute type and local with appropriate predicted index
            localToNewMethodLocalId.put(varExpr.getLocal(), header);

            // Add the parameter to the method
            parameter.addParameter(varExpr.getType());
            params.add(varExpr.getLocal());
            types.put(varExpr.getLocal(), varExpr.getType());

            // Increment by the adequate stack size
            header += TypeUtil.size(varExpr.getType());
        }

        // Create a new method with all the cool kids stuff
        int access = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
        if (methodNode.isStatic()) {
            access |= Opcodes.ACC_STATIC;
        }
        final SkidMethodNode outlinedNode = methodNode
                .getParent()
                .createMethod()
                .name(RandomUtil.randomAlphabeticalString(16))
                .desc(parameter.getDesc())
                .access(access)
                .build();

        // Remove statements from their parent block
        for (Stmt stmt : stmts) {
            stmt.getBlock().remove(stmt);
        }

        // Add the statements to the new cfg
        final SkidControlFlowGraph outlinedCfg = outlinedNode.getCfg();
        outlinedCfg.getEntry().addAll(0, stmts); // Add stmts at beginning
        outlinedCfg.getEntry().add(new ReturnStmt());

        // Create locals
        final Map<Integer, Local> newIndexToLocalsMap = new HashMap<>();
        for (Integer value : localToNewMethodLocalId.values()) {
            final Local local = outlinedCfg.getLocals().get(value);
            newIndexToLocalsMap.put(value, local);
        }

        // Sanitize locals and transfer to new local index map
        for (VarExpr call : calls) {
            final int newIndex = localToNewMethodLocalId.get(call.getLocal());
            final Local newLocal = newIndexToLocalsMap.get(newIndex);

            call.setLocal(newLocal);
        }

        return new ExtrusionResult(outlinedNode, params, types);
    }

    static class ExtrusionResult {
        private final SkidMethodNode methodNode;
        private final List<Local> params;
        private final Map<Local, Type> types;

        public ExtrusionResult(SkidMethodNode methodNode, List<Local> params, Map<Local, Type> types) {
            this.methodNode = methodNode;
            this.params = params;
            this.types = types;
        }

        public SkidMethodNode getMethodNode() {
            return methodNode;
        }

        public List<Local> getParams() {
            return params;
        }

        public Map<Local, Type> getTypes() {
            return types;
        }

        public Type getType(final Local local) {
            return types.get(local);
        }
    }
}
