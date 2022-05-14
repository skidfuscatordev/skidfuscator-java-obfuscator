package org.mapleir.ir.code;

import java.util.LinkedHashMap;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.*;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.objectweb.asm.Type;

public class StmtTests extends UnitSubTestBase {

	public void testArrayStoreStmt() throws Throwable {
		Expr[] es = es(3);
		ArrayStoreStmt stmt = new ArrayStoreStmt(es[0], es[1], es[2], null);
		testUnit(stmt, es, new String[] {"arrayExpression", "indexExpression", "valueExpression"});
	}
	
	public void testConditionalJumpStmt() throws Throwable {
		Expr[] es = es(2);
		ConditionalJumpStmt stmt = new ConditionalJumpStmt(es[0], es[1], null, null);
		testUnit(stmt, es, new String[] {"left", "right"});
	}
	
	public void testFieldStoreStmt() throws Throwable {
		{
			Expr[] es = es(1);
			FieldStoreStmt stmt = new FieldStoreStmt(null, es[0], "owner", "name", "desc", true);
			testUnit(stmt, es, new String[] {"valueExpression"});
		}
		{
			Expr[] es = es(2);
			FieldStoreStmt stmt = new FieldStoreStmt(es[0], es[1], "owner", "name", "desc", false);
			testUnit(stmt, es, new String[] {"instanceExpression", "valueExpression"});
		}
	}
	
	public void testMonitorStmt() throws Throwable {
		Expr[] es = es(1);
		MonitorStmt stmt = new MonitorStmt(es[0], null);
		testUnit(stmt, es, new String[] {"expression"});
	}
	
	public void testNopStmt() throws Throwable {
		NopStmt stmt = new NopStmt();
		testUnit(stmt, new Expr[0], new String[0]);
	}
	
	public void testPopStmt() throws Throwable {
		Expr[] es = es(1);
		PopStmt stmt = new PopStmt(es[0]);
		testUnit(stmt, es, new String[] {"expression"});
	}
	
	public void testReturnStmt() throws Throwable {
		Expr[] es = es(1);
		ReturnStmt stmt = new ReturnStmt(TypeUtils.OBJECT_TYPE, es[0]);
		testUnit(stmt, es, new String[] {"expression"});
	}
	
	public void testSwitchStmt() throws Throwable {
		Expr[] es = es(1);
		SwitchStmt stmt = new SwitchStmt(es[0], new LinkedHashMap<>(), null);
		testUnit(stmt, es, new String[] {"expression"});
	}
	
	public void testThrowStmt() throws Throwable {
		Expr[] es = es(1);
		ThrowStmt stmt = new ThrowStmt(es[0]);
		testUnit(stmt, es, new String[] {"expression"});
	}
	
	public void testUnconditionalJumpStmt() throws Throwable {
		UnconditionalJumpStmt stmt = new UnconditionalJumpStmt(null, null);
		testUnit(stmt, new Expr[0], new String[0]);
	}
	
	public void testNonSynthCopyStmt() throws Throwable {
		VarExpr v = new VarExpr(new VersionedLocal(1, 0), Type.INT_TYPE);
		Expr e = e();
		CopyVarStmt stmt = new CopyVarStmt(v, e);
		assertEquals(v, stmt.getVariable());
		assertEquals(e, stmt.getExpression());
		VarExpr v2 = new VarExpr(new VersionedLocal(2, 0), Type.INT_TYPE);
		Expr e2 = e();
		stmt.setVariable(v2);
		stmt.setExpression(e2);
		assertEquals(v2, stmt.getVariable());
		assertEquals(e2, stmt.getExpression());
		testOutOfBoundsWrite(stmt, stmt.getClass());
	}
	
	public void testSynthCopyStmt() throws Throwable {
		VarExpr v = new VarExpr(new VersionedLocal(1, 0), Type.INT_TYPE);
		CopyVarStmt stmt = new CopyVarStmt(v, v, true);
		assertEquals(v, stmt.getVariable());
		assertEquals(v, stmt.getExpression());
		testOutOfBoundsWrite(stmt, stmt.getClass());
	}
}
