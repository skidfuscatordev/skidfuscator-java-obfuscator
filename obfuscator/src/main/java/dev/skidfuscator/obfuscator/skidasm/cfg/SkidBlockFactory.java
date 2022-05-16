package dev.skidfuscator.obfuscator.skidasm.cfg;

import dev.skidfuscator.obfuscator.skidasm.expr.SkidConstantExpr;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidSwitchStmt;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.DefaultBlockFactory;
import org.mapleir.ir.cfg.builder.ssa.BlockBuilder;
import org.mapleir.ir.cfg.builder.ssa.expr.ConstantExprBuilder;
import org.mapleir.ir.cfg.builder.ssa.stmt.SwitchStmtBuilder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.objectweb.asm.Type;

import java.util.LinkedHashMap;

public class SkidBlockFactory extends DefaultBlockFactory {
    public static final SkidBlockFactory INSTANCE = new SkidBlockFactory();

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
                    return new ConstantExpr(cst);
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
}
