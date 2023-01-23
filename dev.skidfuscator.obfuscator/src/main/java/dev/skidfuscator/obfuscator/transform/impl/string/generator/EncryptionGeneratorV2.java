package dev.skidfuscator.obfuscator.transform.impl.string.generator;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;

public interface EncryptionGeneratorV2 {
    Expr encrypt(ConstantExpr input, final SkidMethodNode node, final BasicBlock block);

    String decrypt(String input, int key);

    void visit(final SkidClassNode node, String name);
}
