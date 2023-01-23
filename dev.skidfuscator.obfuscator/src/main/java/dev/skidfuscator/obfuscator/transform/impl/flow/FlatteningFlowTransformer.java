package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.FinalMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.*;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.CastExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class FlatteningFlowTransformer extends AbstractTransformer {

    public FlatteningFlowTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "Control Flow Flattening");

        this.initWrappers();
    }

    private final Map<Type, TypeWrapper> wrappers = new HashMap<>();

    void initWrappers() {
        wrappers.put(Type.BOOLEAN_TYPE, new PrimitiveTypeWrapper(
                    Type.BOOLEAN_TYPE,
                    "java/lang/Boolean",
                "booleanValue"
                )
        );
        wrappers.put(Type.CHAR_TYPE, new PrimitiveTypeWrapper(
                        Type.BYTE_TYPE,
                        "java/lang/Byte",
                        "byteValue"
                )
        );
        wrappers.put(Type.BYTE_TYPE, new PrimitiveTypeWrapper(
                        Type.BYTE_TYPE,
                        "java/lang/Byte",
                        "byteValue"
                )
        );
        wrappers.put(Type.SHORT_TYPE, new PrimitiveTypeWrapper(
                        Type.SHORT_TYPE,
                        "java/lang/Short",
                        "shortValue"
                )
        );
        wrappers.put(Type.INT_TYPE, new PrimitiveTypeWrapper(
                        Type.INT_TYPE,
                        "java/lang/Integer",
                        "intValue"
                )
        );
        wrappers.put(Type.LONG_TYPE, new PrimitiveTypeWrapper(
                        Type.LONG_TYPE,
                        "java/lang/Long",
                        "longValue"
                )
        );
        wrappers.put(Type.FLOAT_TYPE, new PrimitiveTypeWrapper(
                        Type.FLOAT_TYPE,
                        "java/lang/Float",
                        "floatValue"
                )
        );
        wrappers.put(Type.DOUBLE_TYPE, new PrimitiveTypeWrapper(
                        Type.DOUBLE_TYPE,
                        "java/lang/Double",
                        "doubleValue"
                )
        );
    }

    private static final Type BOOLEAN = Type.getType(Boolean.class);
    private static final Type CHAR = Type.getType(Character.class);
    private static final Type BYTE = Type.getType(Byte.class);
    private static final Type SHORT = Type.getType(Short.class);
    private static final Type INT = Type.getType(Integer.class);
    private static final Type FLOAT = Type.getType(Float.class);
    private static final Type LONG = Type.getType(Long.class);
    private static final Type DOUBLE = Type.getType(Double.class);

    @Listen
    void handle(final InitMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        final ControlFlowGraph cfg = methodNode.getCfg();
        /*for (BasicBlock block : new ArrayList<>(cfg.vertices())) {
            final BasicBlock immediate = cfg.getImmediate(block);

            if (immediate == null)
                continue;

            cfg.removeEdge(cfg.getImmediateEdge(block));

            final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(block, immediate);
            cfg.addEdge(edge);
            block.add(new UnconditionalJumpStmt(immediate, edge));
        }*/

        final AtomicInteger fixed = new AtomicInteger();

        for (BasicBlock vertex : cfg.vertices()) {
            CopyVarStmt currentStmt = null;
            AllocObjectExpr currentAllocation = null;
            Set<Local> currentLocal = null;

            for (Stmt stmt : new ArrayList<>(vertex)) {
                if (stmt instanceof CopyVarStmt) {
                    final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;
                    if (copyVarStmt.getExpression() instanceof AllocObjectExpr) {
                        currentStmt = copyVarStmt;
                        currentAllocation = (AllocObjectExpr) copyVarStmt.getExpression();
                        currentLocal = new HashSet<>(
                                Collections.singletonList(copyVarStmt.getVariable().getLocal())
                        );

                        System.out.println("Found allocation for " + currentAllocation.getType() + " of " + copyVarStmt);
                    } else if (copyVarStmt.getExpression() instanceof VarExpr && ((VarExpr) copyVarStmt.getExpression()).getLocal().equals(currentLocal)) {
                        System.out.println("Found synthetic from " + copyVarStmt);
                        currentLocal.add(copyVarStmt.getVariable().getLocal());
                    }
                } else if (stmt instanceof PopStmt) {
                    final PopStmt popStmt = (PopStmt) stmt;
                    //System.out.println("Found pop " + popStmt + " with alloc " + currentAllocation);
                    if (currentAllocation != null &&
                            popStmt.getExpression() instanceof VirtualInvocationExpr) {
                        final VirtualInvocationExpr invoke = (VirtualInvocationExpr) popStmt.getExpression();
                        //System.out.println("Mhmm?");
                        if (invoke.getName().equals("<init>")) {
                            //System.out.println("Found virtual invoke " + invoke + " of args " + Arrays.toString(invoke.getArgumentExprs()));
                        }
                        if (invoke.getArgumentExprs()[0] instanceof VarExpr) {
                            final VarExpr varExpr = (VarExpr) invoke.getArgumentExprs()[0];

                            //System.out.println("Matching " + currentLocal + " with " + varExpr.getLocal());
                            if (currentLocal.contains(varExpr.getLocal())) {
                                //System.out.println("Found initializer " + invoke);
                                final Expr[] args = new Expr[invoke.getPrintedArgs().length];
                                for (int i = 0; i < args.length; i++) {
                                    args[i] = invoke.getPrintedArgs()[i];
                                    //System.out.println(" " + i + " ->  " + args[i]);
                                    assert args[i] != null : "Null argument at index " + i + "?: "
                                            + Arrays.asList(invoke.getPrintedArgs());
                                }

                                for (int i = 0; i < args.length; i++) {
                                    args[i].unlink();
                                }

                                invoke.unlink();
                                final int index = vertex.indexOf(stmt);
                                System.out.println("---------------");
                                vertex.add(index, new CopyVarStmt(
                                        currentStmt.getVariable().copy(),
                                        new InitialisedObjectExpr(
                                                currentAllocation
                                                        .getType()
                                                        .getClassName()
                                                        .replace(".", "/"),
                                                invoke.getDesc(),
                                                args
                                        )
                                ));
                                vertex.remove(popStmt);
                                vertex.remove(stmt);
                                currentAllocation = null;
                                currentLocal = null;
                                currentStmt = null;
                                fixed.incrementAndGet();
                            }
                        }
                    }
                } else {
                    for (Expr expr : stmt.enumerateOnlyChildren()) {
                        if (expr instanceof VarExpr) {
                            final VarExpr varExpr = (VarExpr) expr;
                            if (currentLocal != null && currentLocal.contains(varExpr.getLocal())) {
                                //System.out.println("(!) --> " + varExpr);
                            }
                        } else if (expr instanceof VirtualInvocationExpr) {
                            final VirtualInvocationExpr virtualInvocationExpr = (VirtualInvocationExpr) expr;

                            if (virtualInvocationExpr.getName().equals("<init>")) {
                                //System.out.println("[!] --> " + virtualInvocationExpr.getRootParent());
                            }
                        }
                    }
                }
            }
        }
        //System.out.println("Fixed " + fixed.get() + " initialisations");
    }

    @Listen
    void handle(final FinalMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        final Skidfuscator skidfuscator = event.getSkidfuscator();

        final SkidControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        if (cfg.size() <= 3) {
            return;
        }

        if (methodNode.isAbstract() || methodNode.isNative() || methodNode.isInit() || methodNode.isClinit())
            return;

        cfg.recomputeEdges();

        final BlockOpaquePredicate opaquePredicate = methodNode.getFlowPredicate();

        assert opaquePredicate != null : "Flow Predicate is null?";
        final PredicateFlowGetter getter = opaquePredicate.getGetter();

        // TODO: Figure why this is happening
        if (getter == null)
            return;

        final Set<BasicBlock> exempt = new HashSet<>();
        for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
            exempt.addAll(range.getNodes());
            exempt.add(range.getHandler());
        }

        exempt.add(cfg.getEntry());

        /*for (BasicBlock vertex : cfg.vertices()) {
            if (vertex.getStack() == null)
                exempt.add(vertex);
        }*/

        if (cfg.vertices()
                .stream()
                .filter(e -> !exempt.contains(e) && !e.isEmpty())
                .flatMap(Collection::stream)
                .noneMatch(e -> e instanceof ConditionalJumpStmt))
            return;


        if (cfg.allExprStream().anyMatch(AllocObjectExpr.class::isInstance))
            return;



        cfg.allExprStream()
                        .filter(AllocObjectExpr.class::isInstance)
                                .map(AllocObjectExpr.class::cast)
                                        .forEach(e -> {
                                            System.out.println(e.getParent().toString());
                                        });
        cfg.allExprStream()
                .filter(CopyVarStmt.class::isInstance)
                .map(CopyVarStmt.class::cast)
                .filter(e -> e.getExpression() instanceof InitialisedObjectExpr)
                .forEach(e -> {

                    System.out.println(e.toString());
                });

        cfg.allExprStream()
                .filter(VarExpr.class::isInstance)
                .map(VarExpr.class::cast)
                .forEach(e -> {
                    final CodeUnit parent = e.getParent();

                    // Just some cleaning up to improve CFG quality
                    if (!e.getType().equals(TypeUtil.OBJECT_TYPE))
                        return;

                    // This case scenario is exclusive to virtual expressions
                    if (parent instanceof VirtualInvocationExpr) {
                        final VirtualInvocationExpr invoke = (VirtualInvocationExpr) parent;

                        // Case 1: VarExpr is the instance expression
                        if (!invoke.isStatic() && invoke.getArgumentExprs()[0].equals(e)) {
                            // Type is the owner
                            e.setType(Type.getObjectType(invoke.getOwner()));
                        }

                        // Case 2: VarExpr is a parameter
                        final Expr[] args = invoke.getParameterExprs();
                        final Type[] types = new Parameter(invoke.getDesc()).getArgs().toArray(new Type[0]);

                        for (int i = 0; i < args.length; i++) {
                            // Find linearly the type in accordance to the invoke description
                            if (args[i].equals(e)) {
                                e.setType(types[i]);
                                break;
                            }
                        }
                    }
                });
        cfg.allExprStream()
                .filter(VirtualInvocationExpr.class::isInstance)
                .map(VirtualInvocationExpr.class::cast)
                .filter(e -> e.getName().equals("<init>"))
                .forEach(e -> {
                    if (e.getArgumentExprs()[0] instanceof VarExpr) {

                    }
                });

        final LinkedHashMap<Integer, BasicBlock> destinations = new LinkedHashMap<>();
        final SkidBlock dispatcherBlock = new SkidBlock(cfg);
        final Set<Local> locals = new HashSet<>();
        cfg.addVertex(dispatcherBlock);


        cfg.allExprStream()
                .filter(VarExpr.class::isInstance)
                .map(VarExpr.class::cast)
                .forEach(e -> {
                    if (e.getLocal().isFlagSet(SkidBlock.FLAG_PROXY))
                        return;

                    if (e.getLocal().getIndex() <= methodNode.getGroup().getStackHeight())
                        return;

                    final Type type = e.getType();
                    final CodeUnit parent = e.getParent();

                    switch (type.getSort()) {
                        case Type.VOID:
                            throw new IllegalStateException("huh?");
                        case Type.CHAR:
                            parent.overwrite(
                                    e,
                                    new CastExpr(
                                            wrappers.get(type).unwrap(e),
                                            Type.BYTE_TYPE
                                    )
                            );
                            break;
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.SHORT:
                        case Type.INT:
                        case Type.FLOAT:
                        case Type.LONG:
                        case Type.DOUBLE: {
                            parent.overwrite(
                                    e,
                                    wrappers.get(type).unwrap(e)
                            );
                            break;
                        }
                        case Type.ARRAY:
                            return;
                        case Type.OBJECT: {
                            e.setParent(null);
                            final Expr wrapped = new CastExpr(e, type);
                            parent.overwrite(
                                    e,
                                    wrapped
                            );
                            e.setParent(wrapped);
                            break;
                        }
                    }
                    e.setType(TypeUtils.OBJECT_TYPE);
                    locals.add(e.getLocal());
                });

        cfg.vertices()
                .stream()
                .flatMap(Collection::stream)
                .filter(CopyVarStmt.class::isInstance)
                .map(CopyVarStmt.class::cast)
                .forEach(stmt -> {
                    if (stmt.getVariable().getLocal().isFlagSet(SkidBlock.FLAG_PROXY))
                        return;
                    if (stmt.getVariable().getLocal().getIndex() <= methodNode.getGroup().getStackHeight())
                        return;

                    if (stmt.getExpression() instanceof AllocObjectExpr)
                        return;

                    final Type oldType = stmt.getType();
                    Expr expr = stmt.getExpression();

                    if (stmt.isSynthetic()) {
                        return;
                    }

                    switch (oldType.getSort()) {
                        case Type.VOID:
                            throw new IllegalStateException("huh?");
                        case Type.CHAR:
                            expr.setParent(null);
                            expr = new CastExpr(expr, Type.BYTE_TYPE);
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.SHORT:
                        case Type.INT:
                        case Type.FLOAT:
                        case Type.LONG:
                        case Type.DOUBLE: {
                            stmt.setExpression(
                                    wrappers.get(oldType).wrap(expr)
                            );
                            break;
                        }
                        case Type.ARRAY:
                            return;
                        case Type.OBJECT: {
                            expr.setParent(null);
                            final Expr wrapped = new CastExpr(expr, TypeUtils.OBJECT_TYPE);
                            stmt.setExpression(wrapped);
                            expr.setParent(wrapped);
                            break;
                        }
                    }
                    locals.add(stmt.getVariable().getLocal());
                    stmt.getVariable().setType(TypeUtils.OBJECT_TYPE);
                });

        new HashSet<>(cfg.vertices())
                .stream()
                .filter(e -> !exempt.contains(e) && !e.isEmpty() && e.isFlagSet(SkidBlock.FLAG_PROXY))
                .flatMap(Collection::stream)
                .filter(e -> e instanceof UnconditionalJumpStmt)
                .filter(e -> e.isFlagSet(SkidBlock.FLAG_PROXY))
                .map(e -> (UnconditionalJumpStmt) e)
                .forEach(e -> {
                    final SkidBlock currentBlock = (SkidBlock) e.getBlock();
                    final SkidBlock oldTarget = (SkidBlock) e.getTarget();

                    /* Replace target block */
                    e.setTarget(dispatcherBlock);

                    /* Replace edge */
                    final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(
                            currentBlock,
                            dispatcherBlock
                            //e.getEdge() == null ? Opcodes.IFEQ : e.getEdge().opcode
                    );

                    if (e.getEdge() != null)
                        cfg.removeEdge(e.getEdge());
                    cfg.addEdge(edge);
                    e.setEdge(edge);

                    final int seed = methodNode.getBlockPredicate(oldTarget);
                    destinations.put(seed, oldTarget);
                    cfg.addEdge(new SwitchEdge<>(dispatcherBlock, oldTarget, seed));
                });

        // We put it here to prevent adding a dead block

        final BasicBlock fuck = Blocks.exception(cfg, "We messed up bogo...");
        for (Local local : locals) {
            cfg.getEntry().add(0, new CopyVarStmt(
                    new VarExpr(local, TypeUtil.OBJECT_TYPE),
                    new ConstantExpr(null, TypeUtil.OBJECT_TYPE)
            ));
        }
        dispatcherBlock.add(new SwitchStmt(
                getter.get(dispatcherBlock),
                destinations,
                fuck
        ));

        cfg.addEdge(new DefaultSwitchEdge<>(dispatcherBlock, fuck));
        cfg.recomputeEdges();

        System.out.println("Test " + DOUBLE.getInternalName());

        /*if (IntegerBlockPredicateRenderer.DEBUG) {
            methodNode.dump();

            try {
                Analyzer<?> analyzer = new Analyzer<>(new SimpleVerifier());
                analyzer.analyzeAndComputeMaxs(methodNode.owner.getName(), methodNode.node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
    }


    static class TypeWrapper {
        protected final Type type;
        private final Function<Expr, Expr> fromToWrapper;
        private final Function<Expr, Expr> wrapperToFrom;

        public TypeWrapper(Type type, Function<Expr, Expr> fromToWrapper, Function<Expr, Expr> wrapperToFrom) {
            this.type = type;
            this.fromToWrapper = fromToWrapper;
            this.wrapperToFrom = wrapperToFrom;
        }

        public Expr wrap(final Expr expr) {
            return fromToWrapper.apply(expr);
        }

        public Expr unwrap(final Expr expr) {
            return wrapperToFrom.apply(expr);
        }
    }

    static class PrimitiveTypeWrapper extends TypeWrapper {
        public PrimitiveTypeWrapper(Type type, String className, String methodName) {
            super(type, new Function<Expr, Expr>() {
                @Override
                public Expr apply(Expr expr) {
                    expr.setParent(null);
                    final Expr wrapped = new CastExpr(
                            new InitialisedObjectExpr(
                                className,
                                "(" + type.getInternalName() + ")V",
                                new Expr[]{
                                        expr
                                }
                            ),
                            TypeUtil.OBJECT_TYPE
                    );
                    expr.setParent(wrapped);
                    return wrapped;
                }
            }, new Function<Expr, Expr>() {
                @Override
                public Expr apply(Expr expr) {
                    expr.setParent(null);
                    final Expr wrapped = new VirtualInvocationExpr(
                            InvocationExpr.CallType.VIRTUAL,
                            new Expr[]{
                                    new CastExpr(
                                            expr,
                                            getReverseWrap(type)
                                    )
                            },
                            getReverseWrap(type).getClassName().replace(".", "/"),
                            methodName,
                            "()" + type.getInternalName()
                    );
                    expr.setParent(wrapped);
                    return wrapped;
                }
            });
        }
    }

    private static Type getReverseWrap(final Type type) {
        switch (type.getSort()) {
            default:
                return type;
            case Type.BOOLEAN: return BOOLEAN;
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.LONG:
            case Type.FLOAT:
            case Type.DOUBLE: return Type.getType(Number.class);
        }
    }
}
