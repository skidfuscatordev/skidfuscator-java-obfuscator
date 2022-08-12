package dev.skidfuscator.obfuscator.predicate.factory;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;

import java.util.function.Supplier;

public interface PredicateFlowGetter {
    Expr get(final BasicBlock vertex);
}
