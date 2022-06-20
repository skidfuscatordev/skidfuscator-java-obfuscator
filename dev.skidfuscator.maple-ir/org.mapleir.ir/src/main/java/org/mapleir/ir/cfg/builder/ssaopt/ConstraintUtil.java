package org.mapleir.ir.cfg.builder.ssaopt;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;

public class ConstraintUtil implements Opcode {

	public static boolean isInvoke(/*Expr e*/ int opcode) {
		// int opcode = e.getOpcode();
		/* INIT_OBJ contains a folded constructor call. */
		return opcode == INVOKE || opcode == INIT_OBJ;
	}

	public static boolean isUncopyable(Expr e) {
		for(Expr c : e.enumerateWithSelf()) {
			int op = c.getOpcode();
			if(isUncopyable0(op)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isUncopyable0(int opcode) {
		switch (opcode) {
			case INVOKE:
			case INIT_OBJ:
			case ALLOC_OBJ:
			case NEW_ARRAY:
			case CATCH:
			case EPHI:
			case PHI:
				return true;
		};
		return false;
	}
	
//	public static int getCost(Statement stmt) {
//		int cost = 0;
//	}
}
