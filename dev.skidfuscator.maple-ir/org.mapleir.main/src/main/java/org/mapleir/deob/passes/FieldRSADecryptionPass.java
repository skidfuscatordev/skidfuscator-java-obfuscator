package org.mapleir.deob.passes;

import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ArithmeticExpr.Operator;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.stmt.FieldStoreStmt;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.FieldNode;
import org.mapleir.asm.MethodNode;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;

public class FieldRSADecryptionPass implements IPass, Opcode {

	private final Map<String, String> fieldLookupCache;
	private final NullPermeableHashMap<String, Set<Number>> constants;
	private final NullPermeableHashMap<String, Set<Number>> dangerConstants;
	private final NullPermeableHashMap<String, Set<Number>> cdecs;
	private final NullPermeableHashMap<String, Set<Number>> cencs;
	private final Map<String, Number[]> pairs;
	
	private AnalysisContext cxt;
	
	public FieldRSADecryptionPass() {
		fieldLookupCache = new HashMap<>();
		constants        = newMap();
		dangerConstants  = newMap();
		cdecs            = newMap();
		cencs            = newMap();
		pairs            = new HashMap<>();
	}
	
	private static String key(String owner, String name, String desc) {
		return owner + "." + name + " " + desc;
	}
	
	private String lookupField(AnalysisContext cxt, String owner, String name, String desc, boolean isStatic) {
		String oldKey = key(owner, name, desc);
		if(fieldLookupCache.containsKey(oldKey)) {
			return fieldLookupCache.get(oldKey);
		} else {
			return lookupField0(cxt, oldKey, owner, name, desc, isStatic);
		}
	}
	
	private String lookupField0(AnalysisContext cxt, String oldKey, String owner, String name, String desc, boolean isStatic) {		
		ClassNode cn = cxt.getApplication().findClassNode(owner);
		
		if(cn == null) {
			String newKey = key(owner, name, desc);
			fieldLookupCache.put(oldKey, newKey);
			return newKey;
		}
		
		for(FieldNode fn : cn.getFields()) {
			if(fn.getName().equals(name) && fn.getDesc().equals(desc) && (Modifier.isStatic(fn.node.access) == isStatic)) {
				String newKey = key(cn.getName(), fn.getName(), fn.getDesc());
				fieldLookupCache.put(oldKey, newKey);
				return newKey;
			}
		}
		
		return lookupField0(cxt, oldKey, cn.node.superName, name, desc, isStatic);
	}
	
	@Override
	public String getId() {
		return "Field-Modulus-Pass";
	}
	
	@Override
	public PassResult accept(PassContext pcxt) {
		this.cxt = pcxt.getAnalysis();
		
		for(MethodNode m : cxt.getIRCache().getActiveMethods()) {
			ControlFlowGraph cfg = cxt.getIRCache().getFor(m);

			for(BasicBlock b : cfg.vertices()) {
				for(Stmt stmt : b) {
					
					for(Expr c : stmt.enumerateOnlyChildren()) {
						if(c.getOpcode() == ARITHMETIC) {
							ArithmeticExpr arith = (ArithmeticExpr) c;
							if(arith.getOperator() == Operator.MUL) {
								Expr l = arith.getLeft();
								Expr r = arith.getRight();
								
								if(r.getOpcode() == CONST_LOAD && l.getOpcode() == FIELD_LOAD) {
									FieldLoadExpr fle = (FieldLoadExpr) l;
									ConstantExpr constt = (ConstantExpr) r;
									
									Number n = (Number) constt.getConstant();
									
									boolean isLong = (n instanceof Long);
									
									if(__eq(n, 1, isLong) || __eq(n, 0, isLong)) {
										continue;
									}
									
									if(n instanceof Integer || n instanceof Long) {
										cdecs.getNonNull(key(fle)).add(n);
									}
								}
							}
						}
					}
					
					
					if(stmt.getOpcode() == FIELD_STORE) {
						FieldStoreStmt fss = (FieldStoreStmt) stmt;
						Expr val = fss.getValueExpression();
						
						if(bcheck1(val)) {
							if(val.getOpcode() == CONST_LOAD) {
								ConstantExpr c = (ConstantExpr) val;
								if(c.getConstant() instanceof Integer || c.getConstant() instanceof Long) {
									Number n = (Number) c.getConstant();
									if(large(n, c.getConstant() instanceof Long)) {
										cencs.getNonNull(key(fss)).add(n);
									}
								}
							}
							continue;
						}
						
						ArithmeticExpr ar = (ArithmeticExpr) val;
						if(ar.getRight().getOpcode() == CONST_LOAD) {
							ConstantExpr c = (ConstantExpr) ar.getRight();
							Number n = (Number) c.getConstant();
							boolean isLong = c.getConstant() instanceof Long;
							if(__eq(n, 1, isLong) || __eq(n, 0, isLong)) {
								continue;
							}
							
							if(ar.getOperator() == Operator.ADD) {
								if(!large(n, isLong)) {
									continue;
								}
							}
							
							cencs.getNonNull(key(fss)).add(n);
						}
						
					}
				}
				
				for(Stmt stmt : b) {
					if(stmt.getOpcode() == FIELD_STORE) {
						if(key((FieldStoreStmt) stmt).equals("co.k I")) {
//							System.out.println("HERE1: " + stmt);
//							
//							System.out.println(cfg);
						}
						handleFss((FieldStoreStmt) stmt);
					}
					
					for(Expr e : stmt.enumerateOnlyChildren()) {
						if(e.getOpcode() == FIELD_LOAD) {
							if(key((FieldLoadExpr) e).equals("co.k I")) {
//								System.out.println("HERE2: " + stmt);
							}
							
							handleFle(stmt, (FieldLoadExpr) e);
						}
					}
				}
			}
		}
		
		Set<String> keys = new HashSet<>();
		keys.addAll(cencs.keySet());
		keys.addAll(cdecs.keySet());
				
		for(String k : keys) {
			boolean _longint = k.endsWith("J");
			
			Set<Number> encs = cencs.getNonNull(k);
			Set<Number> decs = cdecs.getNonNull(k);
			
			try {
				Number[] pair = get_pair(encs, decs, constants.getNonNull(k), _longint);
				if(pair.length != 2) {
					Set<Number> extended = new HashSet<>(constants.getNonNull(k));
					extended.addAll(dangerConstants.getNonNull(k));
					
					pair = get_pair(encs, decs, extended, _longint);
				}
				
				if(pair.length != 2) {
//					System.out.println("No pair for: " + k);
//					System.out.println("Constants: " + constants.getNonNull(k));
//					System.out.println("Dconsts  : " + dangerConstants.getNonNull(k));
//					System.out.println("Encs     : " + encs);
//					System.out.println("Decs     : " + decs);
				} else {
					pairs.put(k, pair);
//					System.out.println("for: " + k + ": " + Arrays.toString(pair));
					
				}
			} catch(IllegalStateException e) {
				System.err.println();
				System.err.println("Constants: " + constants.getNonNull(k));
				System.out.println("Dconsts  : " + dangerConstants.getNonNull(k));
				System.err.println("Encs     : " + encs);
				System.err.println("Decs     : " + decs);
				System.err.println("key: " + k);
				throw e;
			}
		}
		
		System.out.printf("  identified %n field encoder/decoder pairs.%n", pairs.size());
		
		transform(cxt);
		
		
		return PassResult.with(pcxt, this).finished().make();
	}
	
	private void transform(AnalysisContext cxt) {
		for(ClassNode cn : cxt.getApplication().iterate()) {
			for(MethodNode m : cn.getMethods()) {
				ControlFlowGraph cfg = cxt.getIRCache().getFor(m);
				
				for(BasicBlock b : cfg.vertices()) {
					for(Stmt stmt : b) {
//						String fsKey = "";
						if(stmt.getOpcode() == Opcode.FIELD_STORE) {
							FieldStoreStmt fs = (FieldStoreStmt) stmt;
							Number[] p = pairs.get(key(fs)); // [enc, dec]
							
							if(p != null) {
								Expr e = fs.getValueExpression();
								e.unlink();
								
								ArithmeticExpr ae = new ArithmeticExpr(new ConstantExpr(p[1], ConstantExpr.computeType(p[1])), e, Operator.MUL);
								fs.setValueExpression(ae);
								
//								fsKey = key(fs);
							}
						}
						
						for(Expr e : stmt.enumerateOnlyChildren()) {
							if(e.getOpcode() == FIELD_LOAD) {
								CodeUnit par = e.getParent();
								
								FieldLoadExpr fl = (FieldLoadExpr) e;

								Number[] p = pairs.get(key(fl)); // [enc, dec]

								if(p == null) {
									continue;
								}
								
//								if(key(fl).equals(fsKey)) {
//									continue;
//								}

								if(par.getOpcode() == ARITHMETIC) {
									ArithmeticExpr ae = (ArithmeticExpr) par;
									if(ae.getRight().getOpcode() == CONST_LOAD) {
										ConstantExpr ce = (ConstantExpr) ae.getRight();
										Number cst = (Number) ce.getConstant();
										Number res = __mul(cst, p[0], p[0].getClass().equals(Long.class));


										
//										if(!__eq(res, 1, p[0].getClass().equals(Long.class))) {
//											System.out.println(cst + " -> " + res);
//											System.out.println("  expr: " + fl.getRootParent());
//										}
										
										par.writeAt(new ConstantExpr(res, ConstantExpr.computeType(res)), par.indexOf(ce));

										continue;

									}
								}

								ArithmeticExpr ae = new ArithmeticExpr(new ConstantExpr(p[0], ConstantExpr.computeType(p[0])), fl.copy(), Operator.MUL);
								par.writeAt(ae, par.indexOf(fl));
							}
						}
					}
				}
			}
		}
		
//		for(ClassNode cn : cxt.getClassTree().getClasses().values()) {
//			for(MethodNode m : cn.getMethods()) {
//				ControlFlowGraph cfg = cxt.getCFGS().getIR(m);
//				
//				for(BasicBlock b : cfg.vertices()) {
//					for(Stmt stmt : b) {
//						for(Expr e : stmt.enumerateOnlyChildren()) {
//							if(e.getOpcode() == Opcode.ARITHMETIC) {
//								ArithmeticExpr ae = (ArithmeticExpr) e;
//								if(ae.getRight().getOpcode() == Opcode.CONST_LOAD) {
//									ConstantExpr c = (ConstantExpr) ae.getRight();
//									Object o = c.getConstant();
//									
//									if(o instanceof Long || o instanceof Integer) {
//										Number n = (Number) o;
//										if(__eq(n, 1, ae.getType().equals(DescType.LONG_TYPE))) {
//											Expr l = ae.getLeft();
//											l.unlink();
//											
//											CodeUnit aePar = ae.getParent();
//											aePar.writeAt(l, aePar.indexOf(ae));
//										} else if(__eq(n, 0, ae.getType().equals(DescType.LONG_TYPE))) {
//											c.unlink();
//											
//											CodeUnit aePar = ae.getParent();
//											aePar.writeAt(c, aePar.indexOf(ae));
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
	}
	
	private List<CodeUnit> getInsns(CodeUnit u, Set<CodeUnit> vis) {
		List<CodeUnit> list = new ArrayList<>();
		
		if(vis.contains(u)) {
			return list;
		}
		
		int op = u.getOpcode();
		
		switch(op) {
			case INVOKE:
			case ARRAY_LOAD:
			case ARRAY_STORE:
			case ALLOC_OBJ:
			case INIT_OBJ:
			case COMPARE:
				return list;
		}
		
		if(Opcode.opclass(op) == CLASS_JUMP) {
			return list;
		}
		
		list.add(u);
		vis.add(u);
		
		for(Expr e : u.getChildren()) {
			list.addAll(getInsns(e, vis));
		}
		
		if(!u.isFlagSet(Stmt.FLAG_STMT)) {
			Expr e = (Expr) u;
			CodeUnit par = e.getParent();
			
			list.addAll(getInsns(par, vis));
		}
		
		return list;
	}
	
	private boolean containsOther(List<CodeUnit> list, String key) {
		for(CodeUnit u : list) {
			if(u.getOpcode() == FIELD_LOAD) {
				FieldLoadExpr fle = (FieldLoadExpr) u;
				if(!key(fle).equals(key)) {
					return true;
				}
			} else if(u.getOpcode() == FIELD_STORE) {
				FieldStoreStmt fse = (FieldStoreStmt) u;
				if(!key(fse).equals(key)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void handleFle(Stmt stmt, FieldLoadExpr fle) {
		if(!isIntField(fle.getDesc())) {
			return;
		}
		List<CodeUnit> list = getInsns(fle, new HashSet<>());
		boolean other = containsOther(list, key(fle));
		
		for(CodeUnit u : list) {
			if(u.getOpcode() == CONST_LOAD) {
				ConstantExpr c = (ConstantExpr) u;
				Object cst = c.getConstant();
				if(cst instanceof Integer || cst instanceof Long) {
					if(large((Number)cst, cst instanceof Long)) {
						if(other) {
							dangerConstants.getNonNull(key(fle)).add((Number)cst);
						} else {
							constants.getNonNull(key(fle)).add((Number)cst);
						}
					}
				}
			}
		}
	}
	
	private void handleFss(FieldStoreStmt fss) {
		if(!isIntField(fss.getDesc())) {
			return;
		}

		List<CodeUnit> list = getInsns(fss, new HashSet<>());
		boolean other = containsOther(list, key(fss));
		
		for(CodeUnit u : list) {
			if(u.getOpcode() == CONST_LOAD) {
				ConstantExpr c = (ConstantExpr) u;
				Object cst = c.getConstant();
				if(cst instanceof Integer || cst instanceof Long) {
					if(large((Number)cst, cst instanceof Long)) {
						if(other) {
							dangerConstants.getNonNull(key(fss)).add((Number)cst);
						} else {
							constants.getNonNull(key(fss)).add((Number)cst);
						}
					}
				}
			}
		}
	}
	
	static boolean bcheck1(Expr e) {
		if(e.getOpcode() == ARITHMETIC) {
			ArithmeticExpr ar = (ArithmeticExpr) e;
			Operator op = ar.getOperator();
			
			return op != Operator.MUL && op != Operator.ADD;
		} else {
			return true;
		}
	}
	
	static boolean isIntField(String desc) {
		return desc.equals("I") || desc.equals("J");
	}
	
	String key(FieldStoreStmt fss) {
		return lookupField(cxt, fss.getOwner(), fss.getName(), fss.getDesc(), fss.getInstanceExpression() == null);
	}
	
	String key(FieldLoadExpr fle) {
		return lookupField(cxt, fle.getOwner(), fle.getName(), fle.getDesc(), fle.getInstanceExpression() == null);
	}
	
	static Number __mul(Number n1, Number n2, boolean _longint) {
		if(_longint) {
			return n1.longValue() * n2.longValue();
		} else {
			return n1.intValue() * n2.intValue();
		}
	}
	
	public static boolean __eq(Number n1, Number n2, boolean _longint) {
		if(_longint) {
			return n1.longValue() == n2.longValue();
		} else {
			return n1.intValue() == n2.intValue();
		}
	}
	
	static boolean __lt(Number n1, Number n2, boolean _longint) {
		if(_longint) {
			return Math.abs(n1.longValue()) < Math.abs(n2.longValue());
		} else {
			return Math.abs(n1.intValue()) < Math.abs(n2.intValue());
		}
	}
	
	static void assert_inv(Number s1, Number s2, boolean _longint) {
		if(_longint) {
			long r = s1.longValue() * s2.longValue();
			if(r != 1L) {
				throw new IllegalStateException(String.format("s1 * s2 != 1 (s1:%s, s2:%s, r:%d)", s1, s2, r));
			}
		} else {
			int r = s1.intValue() * s2.intValue();
			if(r != 1) {
				throw new IllegalStateException(String.format("s1 * s2 != 1 (s1:%s, s2:%s, r:%d)", s1, s2, r));
			}
		}
	}
	
	public static Number[] get_pair(Collection<Number> encs, Collection<Number> decs, Set<Number> all, boolean _longint) {
//		List<Number> all = new ArrayList<>();
//		all.addAll(encs);
//		all.addAll(decs);
		
		/* p = encoder, c = const
		 * q = decoder, d = const
		 * 
		 * cp * dq (mod 2^32) == cd
		 * 
		 * to solve for p and q, find the closest, non zero
		 * value, to 1 for cp * dq (mod 2^32) where either c
		 * or d are 1.
		 */
		
		Number smallest = 0;
		Number c1 = 0, c2 = 0;
		for(Number p : all) {
			for(Number q : all) {					
				/* find the closest inverse product to 1*/
				Number r = __mul(p, q, _longint);
				if(__eq(p, 0, _longint) || __eq(q, 0, _longint)  || __eq(r, 0, _longint)) {
					continue;
				}
				
				if(__eq(smallest, 0, _longint) /* no result yet*/ || __eq(r, 1, _longint) || __lt(r, smallest, _longint) /* found a new smaller one */) {
					c1 = p;
					c2 = q;
					smallest = r;
				}
			}
		}
		
//		System.out.println("  smallest: " + smallest + " " + _longint);

		if(!__eq(smallest, 1, _longint)) {
			if(valid(smallest, _longint)) {
				if(valid(c1, _longint)) {
					c2 = invert(smallest, c2, _longint);
					assert_inv(c1, c2, _longint);
				} else if(valid(c2, _longint)) {
					c1 = invert(smallest, c1, _longint);
					assert_inv(c1, c2, _longint);
				} else {
					/* can't. */
					return new Number[0];
				}
			} else {
				if(valid(c1, _longint)) {
					Number is1 = asNumber(inverse(asBigInt(c1, _longint), _longint), _longint);
					if(__eq(__mul(is1, smallest, _longint), c2, _longint)) {
						c2 = is1;
						assert_inv(c1, c2, _longint);
					}
				} else if(valid(c2, _longint)) {
					Number is2 = asNumber(inverse(asBigInt(c2, _longint), _longint), _longint);
					if(__eq(__mul(is2, smallest, _longint), c1, _longint)) {
						c1 = is2;
						assert_inv(c1, c2, _longint);
					}
				} else {
					/* can't. */
					return new Number[0];
				}
			}
		}
		
		
		/* find out which one is the
		 * encoder and which is the
		 * decoder. */
		boolean b1 = resolves(decs, c1, _longint);
		boolean b2 = resolves(decs, c2, _longint);
		
		boolean b3;
		
		if(b1 == b2) {
			b3 = true;
			
			b1 = resolves(encs, c1, _longint);
			b2 = resolves(encs, c2, _longint);
		} else {
			b3 = false;
		}
		
		if(b1 != b2) {
			Number enc, dec;
			if(b1 != b3) {
				enc = c2;
				dec = c1;
			} else {
				enc = c1;
				dec = c2;
			}
			return new Number[] {enc, dec};
		} else {
			return new Number[0];
		}
	}
	
	static BigInteger asBigInt(Number n, boolean _longint) {
		return BigInteger.valueOf(_longint ? n.longValue() : n.intValue());
	}
	
	static Number asNumber(BigInteger n, boolean _longint) {
		if(_longint) {
			return n.longValue();
		} else {
			return n.intValue();
		}
	}
	
	public static BigInteger inverse(BigInteger v, boolean _longint) {
		return v.modInverse(BigInteger.ONE.shiftLeft(_longint ? 64 : 32));
	}
	
	static boolean valid(Number n, boolean _longint) {
		try {
			inverse(asBigInt(n, _longint), _longint);
			return true;
		} catch(ArithmeticException e) {
			return false;
		}
	}
	
	static Number invert(Number smallest, Number c, boolean _longint) {
		if(_longint) {
			return inverse(asBigInt(smallest, _longint), _longint).longValue() * c.longValue();
		} else {
			return inverse(asBigInt(smallest, _longint), _longint).intValue() * c.intValue();
		}
	}
	
	static boolean resolves(Collection<Number> multis, Number i, boolean _longint) {
		if(multis.contains(i)) {
			return true;
		} else {
			BigInteger inv = inverse(asBigInt(i, _longint), _longint);
			for(Number m : multis) {
				Number r = __mul(asNumber(inv, _longint), m, _longint);
				
				if(!large(r, _longint)) {
					return true;
				}
			}
			return false;
		}
	}
	
	static boolean large(Number n, boolean _longint) {
		if(_longint) {
			long v = n.longValue();
			if ((v & 0x8000000000000000L) != 0L) {
				v = ~v + 1L;
			}
			
			return (v & 0x7FF0000000000000L) != 0L;
		} else {
			int v = n.intValue();
			if ((v & 0x80000000) != 0) {
				v = ~v + 1;
			}

			return (v & 0x7FF00000) != 0;
		}
	}
	
	private static NullPermeableHashMap<String, Set<Number>> newMap() {
		return new NullPermeableHashMap<>(HashSet::new);
	}
}
