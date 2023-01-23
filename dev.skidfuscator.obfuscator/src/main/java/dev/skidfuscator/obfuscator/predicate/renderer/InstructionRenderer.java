package dev.skidfuscator.obfuscator.predicate.renderer;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.ir.code.CodeUnit;

public interface InstructionRenderer<T extends CodeUnit> {
    void transform(final Skidfuscator base, final T instruction);
}
