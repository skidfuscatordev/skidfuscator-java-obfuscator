package org.mapleir.ir.code;

import org.mapleir.ir.code.expr.*;
import org.objectweb.asm.Type;

public class ExprTests extends UnitSubTestBase {
	
	public void testAllocObjectExpr() throws Throwable {
		AllocObjectExpr expr = new AllocObjectExpr(null);
		testUnit(expr, new Expr[0], new String[0]);
	}
	
	public void testArithmeticExpr() throws Throwable {
		Expr[] es = es(2);
		ArithmeticExpr expr = new ArithmeticExpr(es[1], es[0], null);
		testUnit(expr, es, new String[] {"left", "right"});
	}
	
	public void testArrayLengthExpr() throws Throwable {
		Expr[] es = es(1);
		ArrayLengthExpr expr = new ArrayLengthExpr(es[0]);
		testUnit(expr, es, new String[] {"expression"});
	}
	
	public void testArrayLoadExpr() throws Throwable {
		Expr[] es = es(2);
		ArrayLoadExpr expr = new ArrayLoadExpr(es[0], es[1], null);
		testUnit(expr, es, new String[] {"arrayExpression", "indexExpression"});
	}
	
	public void testCastExpr() throws Throwable {
		Expr[] es = es(1);
		CastExpr expr = new CastExpr(es[0], null);
		testUnit(expr, es, new String[] {"expression"});
	}
	
	public void testCaughtExceptionExpr() throws Throwable {
		CaughtExceptionExpr expr = new CaughtExceptionExpr((Type) null);
		testUnit(expr, new Expr[0], new String[0]);
	}
	
	public void testComparisonExpr() throws Throwable {
		Expr[] es = es(2);
		ComparisonExpr expr = new ComparisonExpr(es[0], es[1], null);
		testUnit(expr, es, new String[] {"left", "right"});
	}
	
	public void testConstantExpr() throws Throwable {
		ConstantExpr expr = new ConstantExpr(null);
		testUnit(expr, new Expr[0], new String[0]);
	}
	
	public void testFieldLoadExpr() throws Throwable {
		{
			FieldLoadExpr expr = new FieldLoadExpr(null, "owner", "name", "desc", true);
			testUnit(expr, new Expr[0], new String[0]);
		}
		{
			Expr[] es = es(1);
			FieldLoadExpr expr = new FieldLoadExpr(es[0], "owner", "name", "desc", false);
			testUnit(expr, es, new String[] {"instanceExpression"});
		}
	}
	
	public void testInstanceofExpr() throws Throwable {
		Expr[] es = es(1);
		InstanceofExpr expr = new InstanceofExpr(es[0], null);
		testUnit(expr, es, new String[] {"expression"});
	}
	
	public void testNegationExpr() throws Throwable {
		Expr[] es = es(1);
		NegationExpr expr = new NegationExpr(es[0]);
		testUnit(expr, es, new String[] {"expression"});
	}
	
	public void testNewArrayExpr() throws Throwable {
		Expr[] es = es(5);
		NewArrayExpr expr = new NewArrayExpr(es, null);
		
		for(int i=0; i < es.length; i++) {
			assertEquals(es[i], expr.read(i));
			Expr e = e();
			expr.writeAt(e, i);
			assertEquals(e, expr.read(i));
		}
		
		testOutOfBoundsWrite(expr, expr.getClass());
	}
	
	public void testPhiExpr() throws Throwable {
		PhiExpr expr = new PhiExpr(null);
		testUnit(expr, new Expr[0], new String[0]);
	}
	
	public void testVarExpr() throws Throwable {
		VarExpr expr = new VarExpr(null, null);
		testUnit(expr, new Expr[0], new String[0]);
	}
}
