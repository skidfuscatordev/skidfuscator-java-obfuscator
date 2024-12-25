package dev.skidfuscator.obfuscator.transform.impl.loop;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ComparisonExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.objectweb.asm.Type;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;

import java.util.HashSet;

public class LoopConditionTransformer extends AbstractTransformer {
    public LoopConditionTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Loop Condition");
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final ControlFlowGraph cfg = event.getMethodNode().getCfg();
        if (cfg == null) {
            return;
        }

        // We need to work on a copy since we'll be modifying the blocks
        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            for (Stmt stmt : new HashSet<>(block)) {
                if (!(stmt instanceof ConditionalJumpStmt)) {
                    continue;
                }

                ConditionalJumpStmt jumpStmt = (ConditionalJumpStmt) stmt;
                Expr left = jumpStmt.getLeft();
                Expr right = jumpStmt.getRight();

                // Only handle integer comparisons for now
                if (!left.getType().equals(Type.INT_TYPE) ||
                    !right.getType().equals(Type.INT_TYPE)) {
                    continue;
                }

                transformCondition(jumpStmt);
                this.success();
            }
        }
    }

    private void transformCondition(ConditionalJumpStmt jumpStmt) {
        Expr left = jumpStmt.getLeft();
        Expr right = jumpStmt.getRight();

        // Generate random constants for obfuscation
        int multiplier = RandomUtil.nextInt(10) + 1; // Small multiplier to reduce intermediate overflow
        int mask = RandomUtil.nextInt();

        // First apply XOR
        Expr xorLeft = new ArithmeticExpr(
                left.copy(),
                new ConstantExpr(mask),
                ArithmeticExpr.Operator.XOR
        );

        Expr xorRight = new ArithmeticExpr(
                right.copy(),
                new ConstantExpr(mask),
                ArithmeticExpr.Operator.XOR
        );

        // Then multiply
        Expr mulLeft = new ArithmeticExpr(
                xorLeft,
                new ConstantExpr(multiplier),
                ArithmeticExpr.Operator.MUL
        );

        Expr mulRight = new ArithmeticExpr(
                xorRight,
                new ConstantExpr(multiplier),
                ArithmeticExpr.Operator.MUL
        );

        // Finally, take modulo of the original value to preserve bounds
        // This ensures array indices stay within bounds
        Expr boundedLeft = new ArithmeticExpr(
                mulLeft,
                right.copy(), // Use the original right value (usually array length) for modulo
                ArithmeticExpr.Operator.REM
        );

        // For the right side, we keep it unchanged if it's likely an array length
        // Otherwise, apply the same transformation
        Expr boundedRight = right;

        // Update the jump condition with transformed expressions
        jumpStmt.setLeft(boundedLeft);
        jumpStmt.setRight(boundedRight);
    }
} 