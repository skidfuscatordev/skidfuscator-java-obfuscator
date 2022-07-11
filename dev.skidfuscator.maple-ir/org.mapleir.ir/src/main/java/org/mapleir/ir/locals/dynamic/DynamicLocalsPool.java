package org.mapleir.ir.locals.dynamic;

import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.TryCatchEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Parameter;
import org.mapleir.ir.locals.SSALocalsPool;
import org.mapleir.ir.locals.impl.BasicLocal;
import org.mapleir.ir.locals.dynamic.DynamicLocal;
import org.mapleir.ir.locals.impl.StaticMethodLocalsPool;
import org.mapleir.ir.locals.impl.VirtualMethodLocalsPool;
import org.mapleir.ir.utils.CFGUtils;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class DynamicLocalsPool {
    private final ApplicationClassSource source;
    private final ControlFlowGraph cfg;
    private final Map<DynamicLocal, LocalCoverage> locals = new HashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    public DynamicLocalsPool(ApplicationClassSource source, ControlFlowGraph cfg) {
        this.source = source;
        this.cfg = cfg;
    }

    public void create() {
        counter.set(0);

        final MethodNode methodNode = cfg.getMethodNode();

        final Map<BasicBlock, Map<Integer, LocalCoverage>> translateCov = new HashMap<>();
        for (BasicBlock vertex : cfg.vertices()) {
            translateCov.put(vertex, new HashMap<>());
        }

        final BasicBlock entryBlock = cfg.getEntries().iterator().next();
        Map<Integer, LocalCoverage> translateEntry = translateCov.get(entryBlock);

        if (!methodNode.isStatic()) {
            final DynamicLocal selfInstanceLocal = new DynamicLocal(counter.getAndIncrement());
            selfInstanceLocal.setType(methodNode.getOwnerType());

            final LocalCoverage coverage = new LocalCoverage(selfInstanceLocal);
            // TODO:    Do coverage

            locals.put(selfInstanceLocal, coverage);
            translateEntry.put(0, coverage);
        }

        final Parameter parameter = new Parameter(methodNode.getDesc());
        int headerHeight = methodNode.isStatic() ? 0 : 1;
        for (Type arg : parameter.getArgs()) {
            final DynamicLocal parameterLocal = new DynamicLocal(counter.getAndIncrement());
            parameterLocal.setType(arg);

            final LocalCoverage coverage = new LocalCoverage(parameterLocal);
            // TODO:    Do coverage

            locals.put(parameterLocal, coverage);
            translateEntry.put(headerHeight, coverage);

            if (arg.equals(Type.DOUBLE_TYPE) || arg.equals(Type.LONG_TYPE)) {
                headerHeight += 2;
                counter.incrementAndGet();
            } else {
                headerHeight += 1;
            }
        }

        for (Map<Integer, LocalCoverage> value : translateCov.values()) {
            value.putAll(translateEntry);
        }

        final Map<BasicBlock, DynamicLocalHeader> headers = new HashMap<>();
        for (BasicBlock vertex : cfg.vertices()) {
            headers.put(vertex, new DynamicLocalHeader(new LocalCoverage[cfg.getLocals().getMaxLocals()]));
        }

        final Set<BasicBlock> visited = new HashSet<>();
        final Stack<BasicBlock> stack = new Stack<>();
        stack.add(entryBlock);

        final DynamicLocalHeader entryHeader = headers.get(entryBlock);
        translateEntry.forEach(entryHeader::set);

        while (!stack.isEmpty()) {
            final BasicBlock popped = stack.pop();
            visited.add(popped);

            final List<BasicBlock> next = new ArrayList<>();
            final Map<Integer, LocalCoverage> translate = translateCov.get(popped);

            for (Stmt stmt : popped) {
                /* Get all the children var exprs */
                // TODO:    Move this to second loop
                for (Expr expr : stmt.enumerateOnlyChildren()) {
                    if (expr instanceof VarExpr) {
                        final VarExpr varExpr = (VarExpr) expr;

                        assert translate.containsKey(varExpr.getIndex()) : "Local used before definition? " +
                                "(local: " + varExpr.getLocal().getIndex()
                                + " desc: " + methodNode.getDesc()
                                + " static: " + methodNode.isStatic()
                                + ")";

                        final LocalCoverage coverage = translate.get(varExpr.getIndex());
                        varExpr.setLocal(coverage.getLocal());
                        coverage.addUse(varExpr);
                    }
                }

                // TODO:    Insert here all the local coverage stuff

                if (stmt instanceof CopyVarStmt) {
                    final CopyVarStmt otherCopyVarStmt = (CopyVarStmt) stmt;
                    final int otherIndex = otherCopyVarStmt.getIndex();
                    final Type otherType = otherCopyVarStmt.getType();


                    // TODO:    do splits here for coverage like in frame
                    //          computer
                    whacko: {
                        if (!translate.containsKey(otherIndex))
                            break whacko;

                        final LocalCoverage other = translate.get(otherIndex);
                        final Type coverageType = other.getLocal().getType();

                        if (false && otherType.getSort() == Type.OBJECT && coverageType.getSort() == Type.OBJECT) {

                            /*final ClassNode selfClassNode = source.findClassNode(o.getInternalName());
                            final ClassNode otherClassNode = source.findClassNode(newest.getInternalName());

                            final ClassNode commonClassNode = source
                                    .getClassTree()
                                    .getCommonAncestor(Arrays.asList(selfClassNode, otherClassNode))
                                    .iterator()
                                    .next();
                            */
                        } else {
                            if (!otherType.equals(coverageType))
                                break whacko;
                        }

                        final LocalCoverage original = translate.get(otherIndex);
                        original.addDef(otherCopyVarStmt);
                        otherCopyVarStmt.getVariable().setLocal(original.local);

                        //translate.put(otherIndex, original);
                        continue;
                    }

                    final DynamicLocal local = new DynamicLocal(counter.getAndIncrement());
                    local.setType(otherType);
                    otherCopyVarStmt.getVariable().setLocal(local);

                    if (otherType.equals(Type.DOUBLE_TYPE) || otherType.equals(Type.LONG_TYPE)) {
                        counter.incrementAndGet();
                    }

                    final LocalCoverage newCov = new LocalCoverage(local);
                    newCov.addDef(otherCopyVarStmt);

                    translate.put(otherIndex, newCov);
                } else {
                    final Map<BasicBlock, Stmt> vars = new HashMap<>();

                    if (stmt instanceof SwitchStmt) {
                        final SwitchStmt switchStmt = (SwitchStmt) stmt;

                        for (BasicBlock value : switchStmt.getTargets().values()) {
                            vars.put(value, stmt);
                        }

                        vars.put(switchStmt.getDefaultTarget(), switchStmt);
                    } else if (stmt instanceof ConditionalJumpStmt) {
                        final ConditionalJumpStmt conditionalJumpStmt = (ConditionalJumpStmt) stmt;
                        vars.put(conditionalJumpStmt.getTrueSuccessor(), stmt);

                    } else if (stmt instanceof UnconditionalJumpStmt) {
                        final UnconditionalJumpStmt unconditionalJumpStmt = (UnconditionalJumpStmt) stmt;
                        vars.put(unconditionalJumpStmt.getTarget(), stmt);
                    }

                    for (BasicBlock basicBlock : vars.keySet()) {
                        translateCov.get(basicBlock).putAll(translate);
                    }
                    next.addAll(vars.keySet());
                }
            }

            final BasicBlock value = cfg.getImmediate(popped);
            if (value != null) {
                next.add(value);

                translateCov.get(value).putAll(translate);
            }

            cfg.getSuccessors(new Predicate<FlowEdge<BasicBlock>>() {
                @Override
                public boolean test(FlowEdge<BasicBlock> basicBlockFlowEdge) {
                    return basicBlockFlowEdge instanceof TryCatchEdge
                            && ((TryCatchEdge) basicBlockFlowEdge)
                            .erange
                            .getNodes()
                            .contains(popped);
                }
            }, popped).forEach(e -> {
                next.add(e.dst());

                translateCov.get(e.dst()).putAll(translate);
            });


            /* Add all the successor nodes */
            /* Add it to the stack to be iterated again */
            next.stream()
                    .filter(d -> !visited.contains(d))
                    .filter(d -> !stack.contains(d))
                    .forEach(stack::add);
        }

        cfg.allExprStream()
                .filter(VarExpr.class::isInstance)
                .map(VarExpr.class::cast)
                .filter(e -> e.getLocal() instanceof BasicLocal)
                .forEach(e -> {
                    System.err.println("Failed to compute index " + e.getLocal().getIndex() + " of block\n" + CFGUtils.printBlock(e.getBlock()));
                });
    }

    public void dump() {
        final SSALocalsPool localsPool = cfg.getMethodNode().isStatic()
                ? new StaticMethodLocalsPool()
                : new VirtualMethodLocalsPool();

        locals.forEach((local, coverage) -> {
            final BasicLocal basicLocal = localsPool.get(local.getIndex());
            basicLocal.setType(local.getType());

            for (VarExpr use : coverage.uses) {
                use.setLocal(basicLocal);
            }

            for (CopyVarStmt def : coverage.defs) {
                def.getVariable().setLocal(basicLocal);
            }
        });

        cfg.getLocals().getCache().clear();
        cfg.getLocals().getCache().putAll(localsPool.getCache());
    }

    public DynamicLocal newLocal(final Type type) {
        final DynamicLocal dynamicLocal = new DynamicLocal(counter.getAndIncrement());
        dynamicLocal.setType(type);

        if (type.equals(Type.DOUBLE_TYPE) || type.equals(Type.LONG_TYPE)) {
            counter.getAndIncrement();
        }

        final LocalCoverage coverage = new LocalCoverage(dynamicLocal);
        locals.put(dynamicLocal, coverage);

        return dynamicLocal;
    }

    public void addDef(final DynamicLocal local, CopyVarStmt create) {
        locals.get(local).addDef(create);
    }

    public void addUse(final DynamicLocal local, VarExpr expr) {
        locals.get(local).addUse(expr);
    }

    static class LocalCoverage {
        private final DynamicLocal local;
        private BasicBlock start;
        private BasicBlock end;
        private final Set<CopyVarStmt> defs;
        private final Set<VarExpr> uses;

        public LocalCoverage(final DynamicLocal local) {
            this.local = local;
            this.defs = new HashSet<>();
            this.uses = new HashSet<>();
        }

        public void addDef(final CopyVarStmt varStmt) {
            this.defs.add(varStmt);
        }

        public void addUse(final VarExpr varExpr) {
            this.uses.add(varExpr);
        }

        public DynamicLocal getLocal() {
            return local;
        }

        public BasicBlock getStart() {
            return start;
        }

        public void setStart(BasicBlock start) {
            this.start = start;
        }

        public BasicBlock getEnd() {
            return end;
        }

        public void setEnd(BasicBlock end) {
            this.end = end;
        }
    }

    static class LocalHeader {
        private final List<Type> types;

        public LocalHeader() {
            this.types = new ArrayList<>();
        }

        public int add(final Type type) {
            for (int i = 0; i < types.size(); i++) {
                final Type indexedType = types.get(i);

                if (indexedType.equals(Type.VOID_TYPE)) {
                    // Lookahead for long and double types
                    if (i + 1 < types.size() && type.equals(Type.DOUBLE_TYPE) || type.equals(Type.LONG_TYPE)) {
                        final Type nextIndexedType = types.get(i + 1);

                        // Check there is a space ahead
                        if (!nextIndexedType.equals(Type.VOID_TYPE)) {
                            continue;
                        }
                    }

                    types.set(i, type);
                    return i;
                } else if (indexedType.equals(Type.DOUBLE_TYPE) || indexedType.equals(Type.LONG_TYPE)) {
                    i++;
                }
            }

            final int index = types.size();
            types.add(type);
            return index;
        }
    }
}
