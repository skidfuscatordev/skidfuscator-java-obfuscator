package org.mapleir.ir.code.expr;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;

public class PhiExceptionExpr extends PhiExpr {

	public PhiExceptionExpr(Map<BasicBlock, Expr> arguments) {
		super(EPHI, arguments);
	}
	
	@Override
	public PhiExpr copy() {
		Map<BasicBlock, Expr> map = new HashMap<>();
		for(Entry<BasicBlock, Expr> e : getArguments().entrySet()) {
			map.put(e.getKey(), e.getValue().copy());
		}
		return new PhiExceptionExpr(map);
	}
	
	@Override
	protected char getPhiType() {
		return '\u03D5';
	}
}