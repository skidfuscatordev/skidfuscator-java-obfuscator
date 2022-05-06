package org.mapleir.deob.intraproc;

import static org.mapleir.ir.TypeUtils.ANY;

import java.lang.invoke.WrongMethodTypeException;
import java.util.HashSet;
import java.util.Set;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ArithmeticExpr.Operator;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.stmt.MonitorStmt;
import org.mapleir.ir.code.stmt.MonitorStmt.MonitorMode;
import org.mapleir.ir.code.stmt.ThrowStmt;
import org.objectweb.asm.Type;

public class DumbExceptionAnalysis implements ExceptionAnalysis, Opcode {

	@Override
	public Set<Type> getPossibleUserThrowables(CodeUnit u) {
		Set<Type> set = new HashSet<>();
		
		if(u.isFlagSet(CodeUnit.FLAG_STMT)) {
			Stmt s = (Stmt) u;
			canThrowStmt(s, set);
			
			for(Expr e : s.enumerateOnlyChildren()) {
				canThrowExpr(e, set);
			}
		} else {
			for(Expr e : ((Expr) u).enumerateWithSelf()) {
				canThrowExpr(e, set);
			}
		}
		
		return set;
	}

	private void canThrowStmt(Stmt u, Set<Type> set) {
		switch(u.getOpcode()) {
			case FIELD_STORE:
				set.add(Type.getType(IncompatibleClassChangeError.class));
				set.add(Type.getType(IllegalAccessError.class));
				break;
			case ARRAY_STORE:
				set.add(Type.getType(NullPointerException.class));
				set.add(Type.getType(IndexOutOfBoundsException.class));
				break;
			case RETURN:
				set.add(Type.getType(IllegalMonitorStateException.class));
				break;
			case THROW: {
				ThrowStmt thr = (ThrowStmt) u;
				Expr e = thr.getExpression();
				
				if(e.getOpcode() == Opcode.CONST_LOAD) {
					ConstantExpr c = (ConstantExpr) e;
					if(c.getConstant() == null) {
						set.add(Type.getType(NullPointerException.class));
					} else {
						throw new IllegalStateException(String.format("%s", thr));
					}
				} else {
					set.add(e.getType());
				}
				set.add(Type.getType(IllegalMonitorStateException.class));
				
				break;
			}
			case MONITOR: {
				set.add(Type.getType(NullPointerException.class));
				if(((MonitorStmt) u).getMode() == MonitorMode.EXIT) {
					set.add(Type.getType(IllegalMonitorStateException.class));
				}
				break;
			}
			/* nothing */
			case POP:
			case COND_JUMP:
			case LOCAL_STORE:
			case PHI_STORE:
			case NOP:
			case UNCOND_JUMP:
			case SWITCH_JUMP:
				break;
				
			default:
				throw new UnsupportedOperationException(String.format("%s: %s", Opcode.opname(u.getOpcode()), u));
		}
	}

	private void canThrowExpr(Expr u, Set<Type> set) {
		switch(u.getOpcode()) {
			case ARRAY_LOAD:
				set.add(Type.getType(NullPointerException.class));
				set.add(Type.getType(IndexOutOfBoundsException.class));
				break;
			case NEW_ARRAY:
				set.add(Type.getType(NegativeArraySizeException.class));
				set.add(Type.getType(IllegalAccessError.class));
				break;
			case ARRAY_LEN:
				set.add(Type.getType(NullPointerException.class));
				break;
			case CAST:
				set.add(Type.getType(NullPointerException.class));
				set.add(Type.getType(ClassCastException.class));
				break;
			case INSTANCEOF:
				set.add(Type.getType(ClassCastException.class));
				break;
			case FIELD_LOAD:{
				// FIXME: depends on the lookup method
				// and field access
				set.add(Type.getType(IncompatibleClassChangeError.class));
				set.add(Type.getType(NullPointerException.class));
				break;
			}
			case ARITHMETIC: {
				ArithmeticExpr ar = (ArithmeticExpr) u;
				Operator op = ar.getOperator();
				
				if(op == Operator.DIV || op == Operator.REM) {
					Type t = ar.getType();
					
					if(t == Type.INT_TYPE || t == Type.LONG_TYPE) {
						set.add(Type.getType(ArithmeticException.class));
					}
				}
				break;
			}
			case INVOKE:
				if (((InvocationExpr) u).isDynamic())
					throw new UnsupportedOperationException(u.toString());
				set.add(ANY);
				
				set.add(Type.getType(Error.class));
				set.add(Type.getType(RuntimeException.class));
				
				set.add(Type.getType(NullPointerException.class));
				set.add(Type.getType(IncompatibleClassChangeError.class));
				set.add(Type.getType(AbstractMethodError.class));
				set.add(Type.getType(UnsatisfiedLinkError.class));
				set.add(Type.getType(IllegalAccessError.class));
				set.add(Type.getType(WrongMethodTypeException.class));
				break;
			case ALLOC_OBJ:
				set.add(Type.getType(InstantiationError.class));
				break;
			case INIT_OBJ:
				set.add(ANY);
				
				set.add(Type.getType(Error.class));
				set.add(Type.getType(RuntimeException.class));
				
				set.add(Type.getType(InstantiationError.class));
				
				set.add(Type.getType(NullPointerException.class));
				set.add(Type.getType(IncompatibleClassChangeError.class));
				set.add(Type.getType(AbstractMethodError.class));
				set.add(Type.getType(UnsatisfiedLinkError.class));
				set.add(Type.getType(IllegalAccessError.class));
				set.add(Type.getType(WrongMethodTypeException.class));
				break;
				
			case COMPARE:
			case NEGATE:
			case PHI:
			case EPHI:
			case LOCAL_LOAD:
			case CONST_LOAD:
			case CATCH:
				break;
				
			default:
				throw new UnsupportedOperationException(String.format("%s: %s", Opcode.opname(u.getOpcode()), u));
		}
	}

	@Override
	public Set<Type> getForcedThrowables(CodeUnit u) {
		throw new UnsupportedOperationException("TODO");
	}
}
