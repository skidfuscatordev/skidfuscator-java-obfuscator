package dev.skidfuscator.obfuscator.predicate.renderer.impl;

import dev.skidfuscator.obfuscator.predicate.renderer.InstructionRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.seed.SeedLoadable;
import org.mapleir.ir.code.CodeUnit;

public abstract class AbstractInstructionRenderer<T extends CodeUnit> implements SeedLoadable, InstructionRenderer<T> {

}
