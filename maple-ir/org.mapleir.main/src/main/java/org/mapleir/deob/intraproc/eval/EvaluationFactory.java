package org.mapleir.deob.intraproc.eval;

import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ComparisonExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.objectweb.asm.Type;

public interface EvaluationFactory {

	EvaluationFunctor<Number> arithmetic(Type t1, Type t2, Type rt, ArithmeticExpr.Operator op);
	
	EvaluationFunctor<Number> negate(Type t);
	
	EvaluationFunctor<Number> cast(Type from, Type to);
	
	EvaluationFunctor<Number> compare(Type lt, Type rt, ComparisonExpr.ValueComparisonType type);
	
	EvaluationFunctor<Boolean> branch(Type lt, Type rt, ConditionalJumpStmt.ComparisonType type);
}