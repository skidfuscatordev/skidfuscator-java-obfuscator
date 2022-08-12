package org.mapleir.ir.code;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public abstract class UnitSubTestBase extends TestCase {

	protected void testOutOfBoundsWrite(CodeUnit u, Class<? extends CodeUnit> unitClass) {
		/* write out of bounds */
		try {
			u.writeAt(e(), u.size());
			fail(String.format("%s didn't catch out of bounds write at index %d", unitClass.getName(), u.size()-1));
		} catch(ChildOutOfBoundsException ex) {
			// pass
		}
	}
	
	protected void testUnit(CodeUnit u, Expr[] fields, String[] names) throws Throwable {
		Class<? extends CodeUnit> unitClass = u.getClass();
		
		assertEquals(fields.length, names.length);
		checkExprFields(u, fields, names);
		for(int i=0; i < names.length; i++) {
			Method m = findSetterMethod(unitClass, names[i]);
			m.invoke(u, fields[i] = e());
		}
		checkExprFields(u, fields, names);
		testOutOfBoundsWrite(u, unitClass);
	}
	
	protected Method findSetterMethod(Class<? extends CodeUnit> unitClass, String fieldName) throws Throwable {
		String methodName = "set" + String.valueOf(Character.toUpperCase(fieldName.charAt(0))) + fieldName.substring(1);
		return unitClass.getDeclaredMethod(methodName, new Class<?>[] {Expr.class});
	}
	
	protected void checkExprFields(CodeUnit u, Expr[] fields, String[] names) throws Throwable {
		Class<? extends CodeUnit> unitClass = u.getClass();
		for(int i=0; i < names.length; i++) {
			Field f = unitClass.getDeclaredField(names[i]);
			f.setAccessible(true);
			assertEquals(String.format("exprField='%s'", names[i]), fields[i], f.get(u));
			f.setAccessible(false);
		}
	}
	
	protected Expr[] es(int n) {
		Expr[] es = new Expr[n];
		for(int i=0; i < n; i++) {
			es[i] = e();
		}
		return es;
	}
	
	protected Expr e() {
		return new FakeExpr();
	}
}
