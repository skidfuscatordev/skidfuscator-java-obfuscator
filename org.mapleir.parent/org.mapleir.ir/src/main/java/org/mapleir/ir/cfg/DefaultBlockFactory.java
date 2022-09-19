package org.mapleir.ir.cfg;

import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.builder.ssa.BlockBuilder;
import org.mapleir.ir.cfg.builder.ssa.CfgBuilder;
import org.mapleir.ir.cfg.builder.ssa.expr.*;
import org.mapleir.ir.cfg.builder.ssa.expr.invoke.StaticInvocationExprBuilder;
import org.mapleir.ir.cfg.builder.ssa.expr.invoke.VirtualInvocationExprBuilder;
import org.mapleir.ir.cfg.builder.ssa.stmt.*;
import org.mapleir.ir.cfg.builder.ssa.stmt.copy.CopyPhiStmtBuilder;
import org.mapleir.ir.cfg.builder.ssa.stmt.copy.CopyVarStmtBuilder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.*;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.*;
import org.mapleir.ir.code.stmt.copy.CopyPhiStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.StaticMethodLocalsPool;
import org.objectweb.asm.Type;

import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultBlockFactory implements SSAFactory {
    public static final SSAFactory INSTANCE = new DefaultBlockFactory();

    @Override
    public CfgBuilder cfg() {
        return new CfgBuilder() {
            private LocalsPool localsPool;
            private MethodNode methodNode;

            @Override
            public CfgBuilder localsPool(LocalsPool localsPool) {
                this.localsPool = localsPool;
                return this;
            }

            @Override
            public CfgBuilder method(MethodNode methodNode) {
                this.methodNode = methodNode;
                return this;
            }

            @Override
            public ControlFlowGraph build() {
                assert localsPool != null : "Locals pool has to not be null";
                assert methodNode != null : "MethodNode cannot be null";
                assert localsPool instanceof StaticMethodLocalsPool == methodNode.isStatic()
                        : "LocalsPool has to have a corresponding assignment (StaticMethodLocalsPool if method is static)";
                return new ControlFlowGraph(localsPool, methodNode);
            }
        };
    }

    @Override
    public BlockBuilder block() {
        return new BlockBuilder() {
            private ControlFlowGraph cfg;

            public BlockBuilder cfg(ControlFlowGraph cfg) {
                this.cfg = cfg;
                return this;
            }

            @Override
            public BasicBlock build() {
                assert cfg != null : "ControlFlowGraph cannot be null!";

                return new BasicBlock(cfg);
            }
        };
    }

    @Override
    public AllocObjectExprBuilder alloc_object_expr() {
        return new AllocObjectExprBuilder() {
            private Type type;

            @Override
            public AllocObjectExprBuilder type(Type type) {
                this.type = type;
                return this;
            }

            @Override
            public AllocObjectExpr build() {
                assert type != null : "Type cannot be null";

                return new AllocObjectExpr(type);
            }
        };
    }

    @Override
    public ArithmeticExprBuilder arithmetic_expr() {
        return new ArithmeticExprBuilder() {
            private Expr left;
            private Expr right;
            private ArithmeticExpr.Operator operator;

            @Override
            public ArithmeticExprBuilder left(Expr left) {
                this.left = left;
                return this;
            }

            @Override
            public ArithmeticExprBuilder right(Expr right) {
                this.right = right;
                return this;
            }

            @Override
            public ArithmeticExprBuilder operator(ArithmeticExpr.Operator operator) {
                this.operator = operator;
                return this;
            }

            @Override
            public ArithmeticExpr build() {
                assert left != null : "Left expression must not be null";
                assert right != null : "Right operand must not be null";
                assert operator != null : "Operator must not be null";

                return new ArithmeticExpr(right, left, operator);
            }
        };
    }

    @Override
    public ArrayLengthExprBuilder array_length_expr() {
        return new ArrayLengthExprBuilder() {
            private Expr expr;

            @Override
            public ArrayLengthExprBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public ArrayLengthExpr build() {
                assert expr != null : "Expression cannot be null";
                return new ArrayLengthExpr(expr);
            }
        };
    }

    @Override
    public ArrayLoadExprBuilder array_load_expr() {
        return new ArrayLoadExprBuilder() {
            private Expr array;
            private Expr index;
            private TypeUtils.ArrayType type;

            @Override
            public ArrayLoadExprBuilder array(Expr expr) {
                this.array = expr;
                return this;
            }

            @Override
            public ArrayLoadExprBuilder index(Expr expr) {
                this.index = expr;
                return this;
            }

            @Override
            public ArrayLoadExprBuilder type(TypeUtils.ArrayType type) {
                this.type = type;
                return this;
            }

            @Override
            public ArrayLoadExpr build() {
                assert array != null : "Array must have a non-null instance";
                assert index != null : "Index cannot be null";
                assert type != null : "Type cannot be null";

                return new ArrayLoadExpr(array, index, type);
            }
        };
    }

    @Override
    public CastExprBuilder cast_expr() {
        return new CastExprBuilder() {
            private Expr expr;
            private Type type;

            @Override
            public CastExprBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public CastExprBuilder type(Type type) {
                this.type = type;
                return this;
            }

            @Override
            public CastExpr build() {
                assert expr != null : "Expression cannot be null";
                assert type != null : "Type cannot be null";

                return new CastExpr(expr, type);
            }
        };
    }

    @Override
    public CaughtExceptionExprBuilder caught_exception_expr() {
        return new CaughtExceptionExprBuilder() {
            private String type;

            @Override
            public CaughtExceptionExprBuilder type(String type) {
                this.type = type;
                return this;
            }

            @Override
            public CaughtExceptionExpr build() {
                assert type != null : "TypeStr cannot be null";

                return new CaughtExceptionExpr(type);
            }
        };
    }

    @Override
    public ComparisonExprBuilder comparison_expr() {
        return new ComparisonExprBuilder() {
            private Expr left;
            private Expr right;
            private ComparisonExpr.ValueComparisonType type;

            @Override
            public ComparisonExprBuilder left(Expr left) {
                this.left = left;
                return this;
            }

            @Override
            public ComparisonExprBuilder right(Expr right) {
                this.right = right;
                return this;
            }

            @Override
            public ComparisonExprBuilder type(ComparisonExpr.ValueComparisonType expr) {
                this.type = expr;
                return this;
            }

            @Override
            public ComparisonExpr build() {
                assert left != null : "Left expression cannot be null";
                assert right != null : "Right expression cannot be null";
                assert type != null : "Type expression cannot be null";

                return new ComparisonExpr(left, right, type);
            }
        };
    }

    @Override
    public ConstantExprBuilder constant_expr() {
        return new ConstantExprBuilder() {
            private Object cst;
            private Type type;
            private boolean check = true;

            @Override
            public ConstantExprBuilder cst(Object cst) {
                this.cst = cst;
                return this;
            }

            @Override
            public ConstantExprBuilder type(Type expr) {
                this.type = expr;
                return this;
            }

            @Override
            public ConstantExprBuilder check(boolean check) {
                this.check = check;
                return this;
            }

            @Override
            public ConstantExpr build() {
                if (type == null) {
                    return new ConstantExpr(cst);
                }
                return new ConstantExpr(cst, type, check);
            }
        };
    }

    @Override
    public VirtualInvocationExprBuilder virtual_invoke_expr() {
        return new VirtualInvocationExprBuilder() {
            private InvocationExpr.CallType callType = InvocationExpr.CallType.VIRTUAL;
            private Expr[] args;
            private String owner;
            private String name;
            private String desc;

            @Override
            public VirtualInvocationExprBuilder callType(InvocationExpr.CallType callType) {
                this.callType = callType;
                return this;
            }

            @Override
            public VirtualInvocationExprBuilder args(Expr[] args) {
                this.args = args;
                return this;
            }

            @Override
            public VirtualInvocationExprBuilder owner(String owner) {
                this.owner = owner;
                return this;
            }

            @Override
            public VirtualInvocationExprBuilder name(String name) {
                this.name = name;
                return this;
            }

            @Override
            public VirtualInvocationExprBuilder desc(String desc) {
                this.desc = desc;
                return this;
            }

            @Override
            public VirtualInvocationExpr build() {
                assert owner != null : "Owner name cannot be null";
                assert name != null : "Name cannot be null";
                assert desc != null : "Description cannot be null";


                return new VirtualInvocationExpr(
                        callType,
                        args,
                        owner,
                        name,
                        desc
                );
            }
        };
    }

    @Override
    public StaticInvocationExprBuilder static_invoke_expr() {
        return new StaticInvocationExprBuilder() {
            private InvocationExpr.CallType callType = InvocationExpr.CallType.STATIC;
            private Expr[] args;
            private String owner;
            private String name;
            private String desc;

            @Override
            public StaticInvocationExprBuilder callType(InvocationExpr.CallType callType) {
                this.callType = callType;
                return this;
            }

            @Override
            public StaticInvocationExprBuilder args(Expr[] args) {
                this.args = args;
                return this;
            }

            @Override
            public StaticInvocationExprBuilder owner(String owner) {
                this.owner = owner;
                return this;
            }

            @Override
            public StaticInvocationExprBuilder name(String name) {
                this.name = name;
                return this;
            }

            @Override
            public StaticInvocationExprBuilder desc(String desc) {
                this.desc = desc;
                return this;
            }

            @Override
            public StaticInvocationExpr build() {
                assert owner != null : "Owner name cannot be null";
                assert name != null : "Name cannot be null";
                assert desc != null : "Description cannot be null";


                return new StaticInvocationExpr(
                        callType,
                        args,
                        owner,
                        name,
                        desc
                );
            }
        };
    }

    @Override
    public FieldLoadExprBuilder field_load_expr() {
        return new FieldLoadExprBuilder() {
            private Expr instance;
            private String owner;
            private String name;
            private String desc;
            private boolean isStatic;

            @Override
            public FieldLoadExprBuilder instance(Expr expr) {
                this.instance = expr;
                return this;
            }

            @Override
            public FieldLoadExprBuilder owner(String owner) {
                this.owner = owner;
                return this;
            }

            @Override
            public FieldLoadExprBuilder name(String name) {
                this.name = name;
                return this;
            }

            @Override
            public FieldLoadExprBuilder desc(String desc) {
                this.desc = desc;
                return this;
            }

            @Override
            public FieldLoadExprBuilder statiz(boolean statiz) {
                this.isStatic = statiz;
                return this;
            }

            @Override
            public FieldLoadExpr build() {
                assert instance != null || isStatic : "Non static method cannot have a null instance";
                assert owner != null : "Owner cannot be null";
                assert name != null : "Name cannot be null";
                assert desc != null : "Description cannot be null";

                return new FieldLoadExpr(instance, owner, name, desc, isStatic);
            }
        };
    }

    @Override
    public InstanceofExprBuilder instance_of_expr() {
        return new InstanceofExprBuilder() {
            private Expr expr;
            private Type type;

            @Override
            public InstanceofExprBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public InstanceofExprBuilder type(Type expr) {
                this.type = expr;
                return this;
            }

            @Override
            public InstanceofExpr build() {
                assert expr != null : "Expression cannot be null";
                assert type != null : "Type cannot be null";

                return new InstanceofExpr(expr, type);
            }
        };
    }

    @Override
    public NegationExprBuilder negation_expr() {
        return new NegationExprBuilder() {
            private Expr expr;

            @Override
            public NegationExprBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public NegationExpr build() {
                assert expr != null : "Expression cannot be null";

                return new NegationExpr(expr);
            }
        };
    }

    @Override
    public NewArrayExprBuilder new_array_expr() {
        return new NewArrayExprBuilder() {
            private Expr[] exprs;
            private Type type;

            @Override
            public NewArrayExprBuilder bounds(Expr[] expr) {
                this.exprs = expr;
                return this;
            }

            @Override
            public NewArrayExprBuilder type(Type expr) {
                this.type = expr;
                return this;
            }

            @Override
            public NewArrayExpr build() {
                assert exprs != null : "Expression stack cannot be null";
                assert type != null : "Type cannot be null";

                return new NewArrayExpr(exprs, type);
            }
        };
    }

    @Override
    public PhiExceptionExprBuilder phi_exception_expr() {
        return new PhiExceptionExprBuilder() {
            private Map<BasicBlock, Expr> arguments;

            @Override
            public PhiExceptionExprBuilder args(Map<BasicBlock, Expr> arguments) {
                this.arguments = arguments;
                return this;
            }

            @Override
            public PhiExceptionExpr build() {
                assert arguments != null : "Arguments cannot be null";

                return new PhiExceptionExpr(arguments);
            }
        };
    }

    @Override
    public PhiExprBuilder phi_expr() {
        return new PhiExprBuilder() {
            private Map<BasicBlock, Expr> arguments;

            @Override
            public PhiExprBuilder args(Map<BasicBlock, Expr> arguments) {
                this.arguments = arguments;
                return this;
            }

            @Override
            public PhiExpr build() {
                assert arguments != null : "Arguments cannot be null";

                return new PhiExpr(arguments);
            }
        };
    }

    @Override
    public VarExprBuilder var_expr() {
        return new VarExprBuilder() {
            private Local local;
            private Type type;

            private boolean lifted;

            @Override
            public VarExprBuilder local(Local local) {
                this.local = local;
                return this;
            }

            @Override
            public VarExprBuilder type(Type expr) {
                this.type = expr;
                return this;
            }

            @Override
            public VarExprBuilder lifted(boolean expr) {
                this.lifted = expr;
                return this;
            }

            @Override
            public VarExpr build() {
                assert local != null : "Local is not null";
                assert lifted || type != null : "Type cannot be null";

                return new VarExpr(local, type);
            }
        };
    }

    @Override
    public CopyPhiStmtBuilder copy_phi_stmt() {
        return new CopyPhiStmtBuilder() {
            private VarExpr varExpr;
            private PhiExpr phiExpr;

            @Override
            public CopyPhiStmtBuilder var(VarExpr varExpr) {
                this.varExpr = varExpr;
                return this;
            }

            @Override
            public CopyPhiStmtBuilder phi(PhiExpr phiExpr) {
                this.phiExpr = phiExpr;
                return this;
            }

            @Override
            public CopyPhiStmt build() {
                assert varExpr != null : "VarExpr cannot be null";
                assert phiExpr != null : "PhiExpr cannot be null";

                return new CopyPhiStmt(varExpr, phiExpr);
            }
        };
    }

    @Override
    public CopyVarStmtBuilder copy_var_stmt() {
        return new CopyVarStmtBuilder() {
            private VarExpr varExpr;
            private Expr expr;
            private boolean synthetic;

            @Override
            public CopyVarStmtBuilder var(VarExpr varExpr) {
                this.varExpr = varExpr;
                return this;
            }

            @Override
            public CopyVarStmtBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public CopyVarStmtBuilder synthetic(boolean synthetic) {
                this.synthetic = synthetic;
                return this;
            }

            @Override
            public CopyVarStmt build() {
                assert varExpr != null : "VarExpr cannot be null";
                assert expr != null : "Expr cannot be null";

                return new CopyVarStmt(varExpr, expr, synthetic);
            }
        };
    }

    @Override
    public ArrayStoreStmtBuilder array_store_stmt() {
        return new ArrayStoreStmtBuilder() {
            private Expr array;
            private Expr index;
            private Expr value;
            private TypeUtils.ArrayType type;

            @Override
            public ArrayStoreStmtBuilder array(Expr expr) {
                this.array = expr;
                return this;
            }

            @Override
            public ArrayStoreStmtBuilder index(Expr index) {
                this.index = index;
                return this;
            }

            @Override
            public ArrayStoreStmtBuilder value(Expr value) {
                this.value = value;
                return this;
            }

            @Override
            public ArrayStoreStmtBuilder type(TypeUtils.ArrayType type) {
                this.type = type;
                return this;
            }

            @Override
            public ArrayStoreStmt build() {
                assert array != null : "ArrayExpr cannot be null";
                assert index != null : "IndexExpr cannot be null";
                assert value != null : "ValueExpr cannot be null";
                assert type != null : "Type cannot be null";

                return new ArrayStoreStmt(array, index, value, type);
            }
        };
    }

    @Override
    public ConditionalJumpStmtBuilder conditional_jump_stmt() {
        return new ConditionalJumpStmtBuilder() {
            private Expr left;
            private Expr right;
            private BasicBlock target;
            private ConditionalJumpStmt.ComparisonType type;
            private ConditionalJumpEdge<BasicBlock> edge;

            @Override
            public ConditionalJumpStmtBuilder left(Expr expr) {
                this.left = expr;
                return this;
            }

            @Override
            public ConditionalJumpStmtBuilder right(Expr expr) {
                this.right = expr;
                return this;
            }

            @Override
            public ConditionalJumpStmtBuilder target(BasicBlock block) {
                this.target = block;
                return this;
            }

            @Override
            public ConditionalJumpStmtBuilder type(ConditionalJumpStmt.ComparisonType type) {
                this.type = type;
                return this;
            }

            @Override
            public ConditionalJumpStmtBuilder edge(ConditionalJumpEdge<BasicBlock> edge) {
                this.edge = edge;
                return this;
            }

            @Override
            public ConditionalJumpStmt build() {
                assert left != null : "LeftExpr cannot be null!";
                assert right != null : "RightExpr cannot be null!";
                assert target != null : "Target cannot be null!";
                assert type != null : "Type cannot be null!";

                return new ConditionalJumpStmt(left, right, target, type, edge);
            }
        };
    }

    @Override
    public FieldStoreStmtBuilder field_store_stmt() {
        return new FieldStoreStmtBuilder() {
            private Expr instance;
            private Expr value;
            private String owner;
            private String name;
            private String desc;
            private boolean isStatic;

            @Override
            public FieldStoreStmtBuilder instance(Expr expr) {
                this.instance = expr;
                return this;
            }

            @Override
            public FieldStoreStmtBuilder value(Expr expr) {
                this.value = expr;
                return this;
            }

            @Override
            public FieldStoreStmtBuilder owner(String owner) {
                this.owner = owner;
                return this;
            }

            @Override
            public FieldStoreStmtBuilder name(String name) {
                this.name = name;
                return this;
            }

            @Override
            public FieldStoreStmtBuilder desc(String desc) {
                this.desc = desc;
                return this;
            }

            @Override
            public FieldStoreStmtBuilder statiz(boolean statiz) {
                this.isStatic = statiz;
                return this;
            }

            @Override
            public FieldStoreStmt build() {
                assert value != null : "ValueExpr cannot be null";
                assert owner != null : "Field Owner cannot be null";
                assert name != null : "Field Name cannot be null";
                assert desc != null : "Field Desc cannot be null";

                return new FieldStoreStmt(instance, value, owner, name, desc, isStatic);
            }
        };
    }

    @Override
    public MonitorStmtBuilder monitor_stmt() {
        return new MonitorStmtBuilder() {
            private Expr expr;
            private MonitorStmt.MonitorMode mode;

            @Override
            public MonitorStmtBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public MonitorStmtBuilder mode(MonitorStmt.MonitorMode expr) {
                this.mode = expr;
                return this;
            }

            @Override
            public MonitorStmt build() {
                assert expr != null : "Monitor is null";
                assert mode != null : "Mode is null";

                return new MonitorStmt(expr, mode);
            }
        };
    }

    @Override
    public NopStmtBuilder nop_stmt() {
        return new NopStmtBuilder() {
            @Override
            public NopStmt build() {
                return new NopStmt();
            }
        };
    }

    @Override
    public PopStmtBuilder pop_stmt() {
        return new PopStmtBuilder() {
            private Expr expr;

            @Override
            public PopStmtBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public PopStmt build() {
                assert expr != null : "Expression cannot be null";

                return new PopStmt(expr);
            }
        };
    }

    @Override
    public ReturnStmtBuilder return_stmt() {
        return new ReturnStmtBuilder() {
            private Type type = Type.VOID_TYPE;
            private Expr expr;

            @Override
            public ReturnStmtBuilder type(Type expr) {
                this.type = expr;
                return this;
            }

            @Override
            public ReturnStmtBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public ReturnStmt build() {
                assert type != null : "Type is null";

                return new ReturnStmt(type, expr);
            }
        };
    }

    @Override
    public SwitchStmtBuilder switch_stmt() {
        return new SwitchStmtBuilder() {
            private Expr expr;
            private LinkedHashMap<Integer, BasicBlock> targets;
            private BasicBlock defaultBlock;

            @Override
            public SwitchStmtBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public SwitchStmtBuilder targets(LinkedHashMap<Integer, BasicBlock> targets) {
                this.targets = targets;
                return this;
            }

            @Override
            public SwitchStmtBuilder defaultTarget(BasicBlock defaultBlock) {
                this.defaultBlock = defaultBlock;
                return this;
            }

            @Override
            public SwitchStmt build() {
                assert expr != null : "Expr cannot be null";
                assert targets != null : "Targets cannot be null";
                assert defaultBlock != null : "DefaultBlock cannot be null";

                return new SwitchStmt(expr, targets, defaultBlock);
            }
        };
    }

    @Override
    public ThrowStmtBuilder throw_stmt() {
        return new ThrowStmtBuilder() {
            private Expr expr;

            @Override
            public ThrowStmtBuilder expr(Expr expr) {
                this.expr = expr;
                return this;
            }

            @Override
            public ThrowStmt build() {
                assert expr != null : "Expression cannot be null";

                return new ThrowStmt(expr);
            }
        };
    }

    @Override
    public UnconditionalJumpStmtBuilder unconditional_jump_stmt() {
        return new UnconditionalJumpStmtBuilder() {
            private BasicBlock target;
            private UnconditionalJumpEdge<BasicBlock> edge;

            @Override
            public UnconditionalJumpStmtBuilder target(BasicBlock target) {
                this.target = target;
                return this;
            }

            @Override
            public UnconditionalJumpStmtBuilder edge(UnconditionalJumpEdge<BasicBlock> edge) {
                this.edge = edge;
                return this;
            }

            @Override
            public UnconditionalJumpStmt build() {
                assert target != null : "Target cannot be null";
                assert edge != null : "Edge cannot be null";

                return new UnconditionalJumpStmt(target, edge);
            }
        };
    }
}
