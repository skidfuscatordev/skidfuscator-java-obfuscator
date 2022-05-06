package org.mapleir.ir.code;

import junit.framework.TestCase;

public class CodeUnitTest extends TestCase {
	
	public void testParent1() {
		for(int i=0; i < 4; i++) {
			testParent1(i);
		}
	}
	
	private void testParent1(int depth) {
		FakeStmt stmt = new FakeStmt();
		FakeExpr lastExpr = new FakeExpr();
		assertEquals(null, lastExpr.getParent());
		stmt.writeAt(lastExpr, 0);
		assertEquals(stmt, lastExpr.getParent());
		
		for(int i=0; i < depth; i++) {
			FakeExpr e = new FakeExpr();
			lastExpr.writeAt(e, 0);
			assertEquals(lastExpr, e.getParent());
			lastExpr = e;
		}
		
		assertEquals(stmt, lastExpr.getRootParent());
	}
	
	public void testParent2() {
		for(int i=0; i < 4; i++) {
			testParent2(i);
		}
	}

	private void testParent2(int depth) {
		FakeStmt stmt = new FakeStmt();
		FakeExpr firstExpr, lastExpr;
		firstExpr = lastExpr = new FakeExpr();
		
		for(int i=0; i < depth; i++) {
			FakeExpr e = new FakeExpr();
			lastExpr.writeAt(e, 0);
			assertEquals(lastExpr, e.getParent());
			lastExpr = e;
		}

		assertEquals(null, firstExpr.getParent());
		stmt.writeAt(firstExpr, 0);
		assertEquals(stmt, lastExpr.getRootParent());
	}
	
	public void testAlreadyHasParent() {
		FakeStmt stmt1 = new FakeStmt();
		FakeStmt stmt2 = new FakeStmt();
		FakeExpr e = new FakeExpr();
		
		stmt1.writeAt(e, 0);
		assertEquals(stmt1.read(0), e);
		try {
			stmt2.writeAt(e, 0);
			fail("already has parent");
		} catch(IllegalStateException ex) {
			// pass
		}
	}
	
	public void testPopulate() {
		FakeStmt stmt = new FakeStmt();
		int numChilds = 25;
		populateFakeCodeUnit(stmt, 0, numChilds);
		assertEquals(numChilds, stmt.size());
	}

	public void testOverwriteInBounds() {
		FakeStmt stmt = new FakeStmt();
		// stmt has 2 children, 3rd in array is null
		// so we shouldn't be able to write past 3rd
		int numChilds = 2;
		populateFakeCodeUnit(stmt, 0, numChilds);
		try {
			stmt.writeAt(new FakeExpr(), numChilds + 1);
			fail("should not write beyond last child");
		} catch(ArrayIndexOutOfBoundsException e) {
			// pass
		}
	}
	
	public void testOverwrite() {
		int numChilds = 5;
		for(int i=0; i < numChilds; i++) {
			testOverwrite(numChilds, i);
		}
	}
	
	private void testOverwrite(int numChilds, int idx) {
		FakeStmt stmt = new FakeStmt();
		populateFakeCodeUnit(stmt, 0, numChilds);
		FakeExpr testExpr = new FakeExpr();
		stmt.writeAt(testExpr, idx);
		
		assertEquals(String.format("at index %d", idx), testExpr, stmt.read(idx));
	}
	
	private static void populateFakeCodeUnit(CodeUnit u, int offset, int numChilds) {
		for(int i=0; i < numChilds; i++) {
			u.writeAt(new FakeExpr(), offset + i);
		}
	}
}
