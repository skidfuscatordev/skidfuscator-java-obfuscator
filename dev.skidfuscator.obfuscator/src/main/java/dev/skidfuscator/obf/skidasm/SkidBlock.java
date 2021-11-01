package dev.skidfuscator.obf.skidasm;

import dev.skidfuscator.obf.number.NumberManager;
import lombok.Data;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

@Data
public class SkidBlock {
    private final int seed;
    private final BasicBlock block;

    public SkidBlock(int publicSeed, BasicBlock block) {
        this.seed = publicSeed;
        this.block = block;
    }

    public void addSeedLoader(final int index, final Local local, final int value, final int target) {
        final Expr load = NumberManager.transform(target, value, new VarExpr(local, Type.INT_TYPE));
        final Stmt set = new CopyVarStmt(new VarExpr(local, Type.INT_TYPE), load);
        if (index < 0)
            block.add(set);
        else
            block.add(index, set);
    }
}
