package dev.skidfuscator.obfuscator.skidasm.cfg;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidConstantExpr;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidSwitchStmt;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.DefaultBlockFactory;
import org.mapleir.ir.cfg.builder.ssa.BlockBuilder;
import org.mapleir.ir.cfg.builder.ssa.CfgBuilder;
import org.mapleir.ir.cfg.builder.ssa.expr.ConstantExprBuilder;
import org.mapleir.ir.cfg.builder.ssa.expr.invoke.StaticInvocationExprBuilder;
import org.mapleir.ir.cfg.builder.ssa.stmt.SwitchStmtBuilder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.StaticMethodLocalsPool;
import org.objectweb.asm.Type;

import java.util.LinkedHashMap;

public class SkidBlockFactory extends DefaultBlockFactory {
    public static SkidBlockFactory INSTANCE = null;

    public static SkidBlockFactory v(final Skidfuscator skidfuscator) {
        if (INSTANCE == null) {
            INSTANCE = new SkidBlockFactory(skidfuscator);
        }
        return INSTANCE;
    }

    private final Skidfuscator skidfuscator;

    public SkidBlockFactory(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
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

                final ClassNode classNode = skidfuscator
                        .getClassSource()
                        .findClassNode(owner);

                if (classNode == null) {
                    //System.out.println("Failed to find " + owner + " in reference path...");
                } else if (classNode.isInterface()){
                    //System.out.println("Class " + owner + " is of version " + classNode.node.version + " (annoying: " + classNode.isAnnoyingVersion() + ")");
                }

                final boolean isInterface = classNode != null && classNode.isInterface();

                return new StaticInvocationExpr(
                        isInterface
                                ? InvocationExpr.CallType.INTERFACE
                                : InvocationExpr.CallType.STATIC,
                        args,
                        owner,
                        name,
                        desc
                );
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

                return new SkidSwitchStmt(expr, targets, defaultBlock);
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
                    return new SkidConstantExpr(cst);
                }
                return new SkidConstantExpr(cst, type, check);
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

                return new SkidBlock(cfg);
            }
        };
    }

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
                return new SkidControlFlowGraph(localsPool, methodNode);
            }
        };
    }
}
