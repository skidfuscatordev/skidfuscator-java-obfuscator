package dev.skidfuscator.obfuscator.predicate.factory;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PredicateFlowSetter extends Function<Expr, Stmt> {
}
