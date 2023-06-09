package dev.skidfuscator.obfuscator.predicate.renderer;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;

public interface InstructionRenderer<T> {
    void transform(final Skidfuscator base, final ControlFlowGraph cfg, final T instruction);
}
