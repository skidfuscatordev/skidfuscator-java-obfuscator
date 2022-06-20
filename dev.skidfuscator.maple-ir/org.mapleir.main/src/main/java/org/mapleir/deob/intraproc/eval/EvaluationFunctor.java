package org.mapleir.deob.intraproc.eval;

public interface EvaluationFunctor<V> {

	V eval(Object... args) throws IllegalArgumentException;
}