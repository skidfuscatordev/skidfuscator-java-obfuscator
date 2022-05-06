package org.mapleir.ir.cfg.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.ExpressionStack;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.util.Printer;

public class GenerationVerifier {

	public static boolean VERIFY = false;
	
	/* Format for stack configs:
	 *  OPCODE:NUM_CONFIGS:CONF_0:CONF_2 ... :CONF_N
	 *
	 * Each config has the following format:
	 *  h_n: expected stack object height
	 *     options: C, B, S, I, F, D, J, Z, O(subclass of Ljava/lang/Object;),
	 *              N(null const), E(subclass of Ljava/lang/Exception;),
	 *              *(anything), 1(1, 2 or 4 byte word), 2(8 byte word).
	 *              O shadows N, X(any single stack obj).
	 *              Prefixing a full type name, O or E with #
	 *              will match the exact class.
	 *              All of the above with the exception of N(null) can
	 *              be prepended with array dimension markers ([).
	 *      object pointers count as 1 byte when considering
	 *       stack deltas.
	 *  h_0| ... |h_n
	 * where h_0 is the top element of the stack
	 * and h_n is the n-1th element of the stack
	 * from the top.
	 *
	 *  before>after where before and after
	 *     are both stack configurations.
	 *
	 * Example:
	 *   1:1:*>N|*    describes ACONST_NULL,
	 *      => opcode = 1;
	 *      => 1 configuration
	 *      => no required pre stack configuration
	 *      => top item of post stack is a null
	 *  */
	private static final String[] STACK_CONFIGS = new String[] {
			"0:0",        // NOP
			"1:1:*>N|*",  // ACONST_NULL
			"2:1:*>I|*",  // ICONST_M1
			"3:1:*>1|*",  // ICONST_0
			"4:1:*>1|*",  // ICONST_1
			"5:1:*>1|*",  // ICONST_2
			"6:1:*>1|*",  // ICONST_3
			"7:1:*>1|*",  // ICONST_4
			"8:1:*>1|*",  // ICONST_5
			"9:1:*>J|*",  // LCONST_0
			"10:1:*>J|*", // LCONST_1
			"11:1:*>F|*", // FCONST_0
			"12:1:*>F|*", // FCONST_1
			"13:1:*>F|*", // FCONST_2
			"14:1:*>D|*", // DCONST_0
			"15:1:*>D|*", // DCONST_1
			
			// B/S IPUSH is sign extended before
			// being pushed... >:(
			"16:1:*>B|*", // BIPUSH
			"17:1:*>S|*", // SIPUSH
			"18:1:*>X|*", // LDC
			
			"21:1:*>I|*", // ILOAD
			"22:1:*>J|*", // LLOAD
			"23:1:*>F|*", // FLOAD
			"24:1:*>D|*", // DLOAD
			"25:1:*>X|*", // ALOAD
			
			"46:1:I|[I|*>I|*", // IALOAD
			"47:1:I|[J|*>J|*", // LALOAD
			"48:1:I|[F|*>F|*", // FALOAD
			"49:1:I|[D|*>D|*", // DALOAD
			"50:1:I|[X|*>X|*", // AALOAD
			// B/S ALOAD are signed extended to
			// int before value is pushed and
			// CALOAD is zero extended, also as
			// an int.
			"51:1:I|[B|*>I|*", // BALOAD
			"52:1:I|[C|*>I|*", // CALOAD
			"53:1:I|[S|*>I|*", // SALOAD
			
			"54:2:1>*:Z>*", // ISTORE
			"55:1:J*>*", // LSTORE
			"56:1:F*>*", // FSTORE
			"57:1:D*>*", // DSTORE
			"58:1:X*>*", // ASTORE
			
			"79:1:I|I|[I|*>*", // IASTORE
			"80:1:J|I|[J|*>*", // LASTORE
			"81:1:F|I|[F|*>*", // FASTORE
			"82:1:D|I|[D|*>*", // DASTORE
			"83:1:X|I|[X|*>*", // AASTORE
			// B/C/S ASTORE values are ints
			// and are truncated to byte/char/
			// /short before being set in the
			// array.
			"84:1:B|I|X|*>*", // BASTORE
			"85:1:C|I|X|*>*", // CASTORE
			"86:1:S|I|X|*>*", // SASTORE
			
			"87:1:1|*>*", // POP
			"88:2:1|1|*>*:2|*>*", // POP2
			// Stack operations are handled in
			// their respective generation
			// methods.
			"89:0", // DUP
			"90:0",   // DUP_X1
			"91:0",   // DUP_X2
			"92:0",   // DUP2
			"93:0",   // DUP2_X1
			"94:0",   // DUP2_X2
			"95:0",    // SWAP
			
			// 96 - 131
			"96:1:1|1|*>I|*", // IADD
			"97:1:J|J|*>J|*", // LADD
			"98:1:F|F|*>F|*", // FADD
			"99:1:D|D|*>D|*", // DADD
			"100:1:1|1|*>I|*", // ISUB
			"101:1:J|J|*>J|*", // LSUB
			"102:1:F|F|*>F|*", // FSUB
			"103:1:D|D|*>D|*", // DSUB
			"104:1:1||*>I|*", // IMUL
			"105:1:J|J|*>J|*", // LMUL
			"106:1:F|F|*>F|*", // FMUL
			"107:1:D|D|*>D|*", // DMUL
			"108:1:1|1|*>I|*", // IDIV
			"109:1:J|J|*>J|*", // LDIV
			"110:1:F|F|*>F|*", // FDIV
			"111:1:D|D|*>D|*", // DDIV
			"112:1:1|1|*>I|*", // IREM
			"113:1:J|J|*>J|*", // LREM
			"114:1:F|F|*>F|*", // FREM
			"115:1:D|D|*>D|*", // DREM
			"120:1:1|1|*>I|*", // ISHL
			"121:1:I|J|*>J|*", // LSHL
			"122:1:1|1|*>I|*", // ISHR
			"123:1:I|J|*>J|*", // LSHR
			"124:1:1|1|*>I|*", // IUSHR
			"125:1:I|J|*>J|*", // LUSHR
			"126:1:1|1|*>I|*", // IAND
			"127:1:J|J|*>J|*", // LAND
			"128:1:1|1|*>I|*", // IOR
			"129:1:J|J|*>J|*", // LOR
			"130:1:1|1|*>I|*", // IXOR
			"131:1:J|J|*>J|*", // LXOR
			"116:1:1|*>1|*", // INEG
			"117:1:J|*>J|*", // LNEG
			"118:1:F|*>F|*", // FNEG
			"119:1:D|*>D|*", // DNEG
			
			"132:1:*>*", // IINC
			
			// 133 - 147
			"133:1:I|*>J|*", // I2L
			"134:1:I|*>F|*", // I2F
			"135:1:I|*>D|*", // I2D
			"136:1:J|*>I|*", // L2I
			"137:1:J|*>F|*", // L2F
			"138:1:J|*>D|*", // L2D
			"139:1:F|*>I|*", // F2I
			"140:1:F|*>J|*", // F2L
			"141:1:F|*>D|*", // F2D
			"142:1:D|*>I|*", // D2I
			"143:1:D|*>J|*", // D2L
			"144:1:D|*>F|*", // D2F
			"145:1:1|*>B|*", // I2B
			"146:1:1|*>C|*", // I2C
			"147:1:1|*>S|*", // I2S
			
			// 148 - 158
			"148:1:J|J|*>I|*", // LCMP
			"149:1:F|F|*>I|*", // FCMPL
			"150:1:F|F|*>I|*", // FCMPG
			"151:1:D|D|*>I|*", // DCMPL
			"152:1:D|D|*>I|*", // DCMPG
			"153:2:I|*>*:Z|*>*", // IFEQ
			"154:2:I|*>*:Z|*>*", // IFNE
			"155:2:I|*>*:Z|*>*", // IFLT
			"156:2:I|*>*:Z|*>*", // IFGE
			"157:2:I|*>*:Z|*>*", // IFGT
			"158:2:I|*>*:Z|*>*", // IFLE
			
			"159:1:1|1|*>*", // IF_ICMPEQ
			"160:1:1|1|*>*", // IF_ICMPNE
			"161:1:1|1|*>*", // IF_ICMPLT
			"162:1:1|1|*>*", // IF_ICMPGE
			"163:1:1|1|*>*", // IF_ICMPGT
			"164:1:1|1|*>*", // IF_ICMPLE
			"165:1:X|X|*>*", // IF_ACMPEQ
			"166:1:X|X|*>*", // IF_ACMPNE
			
			"167:0", // GOTO
			// JSR and RET omitted (unsupported)
			"170:1:I|*>*", // TABLESWITCH
			"171:1:I|*>*", // LOOKUPSWITCH
			
			"172:1:I|*>", // IRETURN
			"173:1:J|*>", // LRETURN
			"174:1:F|*>", // FRETURN
			"175:1:D|*>", // DRETURN
			"176:1:X|*>", // ARETURN
			"177:1:>", // RETURN
			
			"178:1:*>X|*", // GETSTATIC
			"179:1:X|*>*", // PUTSTATIC
			"180:1:O|*>X|*", // GETFIELD
			"181:1:X|O|*>*", // PUTFIELD
			
			// TODO: add dynamic getters for
			//       variable length insns.
	};
	
	/* public static void main(String[] args) {
		// 96 IADD - 115 DREM (2)
		// 116-119 I,L,F,D NEG (1)
		// 120-131 shift ops (2)

		// N.B: These convert long to L instead of J.
		gen_arith_configs(96, 115, 2);
		gen_arith_configs(120, 131, 2);
		gen_arith_configs(116, 119, 1);
		gen_cast_configs(133, 147);
		gen_cmp_configs(148, 158);
	}
	private static void gen_arith_configs(int f, int l, int s) {
		for(int i=f; i <= l; i++) {
			String n = Printer.OPCODES[i];
			char t = Character.toUpperCase(n.charAt(0));
			
			String pre = "";
			for(int j=0; j < s; j++) {
				pre += t;
				pre += "|";
			}
			pre += "*";
			
			String post = t + "|*";
			
			String p = String.format("\"%d:1:%s>%s\", // %s", i, pre, post, n);
			System.out.println(p);;
		}
	}
	private static void gen_cast_configs(int f, int l) {
		for(int i=f; i <= l; i++) {
			String n = Printer.OPCODES[i];
			char ft = Character.toUpperCase(n.charAt(0));
			char tt = Character.toUpperCase(n.charAt(2));
			
			String pre = ft + "|*";
			String post = tt + "|*";
			
			String p = String.format("\"%d:1:%s>%s\", // %s", i, pre, post, n);
			System.out.println(p);
		}
	}
	private static void gen_cmp_configs(int f, int l) {
		for(int i=f; i <= l; i++) {
			String n = Printer.OPCODES[i];
			char t = Character.toUpperCase(n.charAt(0));
			
			String pre = t + "";
			if(n.contains("CMP")) {
				pre += "|" + t;
			}
			pre += "|*";
			
			String post = "I|*";
			
			String p = String.format("\"%d:1:%s>%s\", // %s", i, pre, post, n);
			System.out.println(p);
		}
	} */
	
	static class VerifierToken {
		static final Map<String, VerifierToken> cache = new HashMap<>();
		
		static final int CLASS_EMPTY = 0;
		static final int CLASS_WORD_LEN = 1;
		static final int CLASS_NULL = 2;
		static final int CLASS_ANY_SINGLE = 3;
		static final int CLASS_TYPE_SUB = 4;
		static final int CLASS_TYPE_EXACT = 5;
		static final int CLASS_ALL = 6;
		static final int CLASS_ANY_SINGLE_CLASS = 3;
		
		final Object tok;
		final int dims;
		final int tclass;
		
		VerifierToken(String t) {
			Object tok = t;
			int dims = 0;
			int tclass = -1;
			
			int len = t.length();
			if(len == 0) {
				tclass = CLASS_EMPTY;
			} else {
				String actualType = t;
				String arrayClean = t.replace("[", "");
				dims = t.length() - arrayClean.length();
				char c = arrayClean.charAt(0);
				
				boolean exact = (c == '#');
				if(exact) {
					// exact
					c = arrayClean.charAt(1);
					actualType = actualType.substring(1);
				}
				
				switch(c) {
					case '1':
					case '2':
						tok = Integer.parseInt(t);
						tclass = CLASS_WORD_LEN;
						break;
					case 'N':
						tclass = CLASS_NULL;
						break;
					case 'X':
						tclass = CLASS_ANY_SINGLE;
						break;
					case 'C':
					case 'B':
					case 'S':
					case 'I':
					case 'F':
					case 'D':
					case 'J':
					case 'Z':
						tok = Type.getType(actualType);
						tclass = CLASS_TYPE_EXACT;
						break;
					case 'O':
						tclass = CLASS_ANY_SINGLE_CLASS;
						break;
					case '*':
						tclass = CLASS_ALL;
						break;
				}
				
				if(tclass == -1) {
					tok = Type.getType("L" + actualType + ";");
					tclass = exact ? CLASS_TYPE_EXACT : CLASS_TYPE_SUB;
				}
				
				if(tclass == -1) {
					throw new UnsupportedOperationException(t);
				}
			}
			
			this.tok = tok;
			this.dims = dims;
			this.tclass = tclass;
		}
		
		boolean matches(Expr e) {
			Type type = e.getType();
			
			if(tclass == CLASS_EMPTY) {
				// We don't add these to the token list
				// so it shouldn't appear here.
				throw new UnsupportedOperationException();
			} else if(tclass == CLASS_WORD_LEN) {
				int s = size(type);
				return s == (int)tok;
			} else if(tclass == CLASS_NULL) {
				if(e.getOpcode() == Opcode.CONST_LOAD) {
					ConstantExpr c = (ConstantExpr) e;
					return c.getConstant() == null;
				} else {
					return false;
				}
			} else if(tclass == CLASS_ANY_SINGLE) {
				return true;
			} else if(tclass == CLASS_ALL) {
				// the matcher omits this token.
				throw new UnsupportedOperationException();
			} else if(tclass == CLASS_TYPE_EXACT) {
				return type.equals(tok);
			} else if(tclass == CLASS_TYPE_SUB) {
				if(dims > 0) {
					String arrayObjType = type.getInternalName();
					arrayObjType = arrayObjType.replace("[", "");
					
					int tdims = type.getInternalName().length() - arrayObjType.length();
					
					if(tdims != dims) {
						return false;
					} else {
						// String tokObjType = ((Type) tok).getInternalName().substring(dims);
						// if(tokObjType.eq)
						
						// TODO: check if arrayObjType extends tokObjType.
						return true;
					}
				} else {
					return type.equals(tok);
				}
			} else if(tclass == CLASS_ANY_SINGLE_CLASS) {
				// TODO: [] fields and methods
				return type.getSort() == Type.OBJECT;
			} else {
				throw new UnsupportedOperationException(e + ",  " + type + ",  " + tclass + ",   " + tok + ",   " + dims);
			}
		}
		
		static int size(Type t) {
			String s = t.toString();
			switch(s) {
				case "D":
				case "J":
					return 2;
				default:
					return 1;
			}
		}
		
		static VerifierToken[] makeTokens(String stack) {
			if(stack.isEmpty()) {
				return new VerifierToken[0];
			} else {
				String[] ss = stack.split("\\|");
				List<VerifierToken> lst = new ArrayList<>();
				
				for(int i=0; i < ss.length; i++) {
					String s = ss[i];
					
					VerifierToken t = null;
					if(cache.containsKey(s)) {
						t = cache.get(s);
					} else {
						t = new VerifierToken(s);
						cache.put(s, t);
					}
					
					if(t.tclass != VerifierToken.CLASS_EMPTY) {
						lst.add(t);
					}
				}
				
				return lst.toArray(new VerifierToken[ss.length]);
			}
		}
	}
	
	static class VerifierRule {
		final VerifierToken[] preTokens;
		final VerifierToken[] postTokens;
		
		VerifierRule(VerifierToken[] preTokens, VerifierToken[] postTokens) {
			this.preTokens = preTokens;
			this.postTokens = postTokens;
		}
		
		boolean match_attempt(ExpressionStack stack, boolean pre) {
			if(pre) {
				return match_attempt(stack, preTokens);
			} else {
				return match_attempt(stack, postTokens);
			}
		}
		
		boolean match_attempt(ExpressionStack stack, VerifierToken[] tokens) {
			if(tokens.length == 0) {
				if(stack.size() != 0) {
					return false;
				}
			} else {
				VerifierToken last = tokens[tokens.length - 1];
				int expectedSize = tokens.length;
				/* Since the all(*) matcher can only appear at the
				 * end of the token sequence, if we have one, we
				 * only check the first n-1 exprs of the stack where
				 * n is the number of elements on the stack as the last
				 * one will match 0 or more elements regardless.*/
				if(last.tclass == VerifierToken.CLASS_ALL/* '*' */) {
					expectedSize = expectedSize - 1;
				}
				
				if(expectedSize > 0) {
					for(int i=0; i < expectedSize; i++) {
						VerifierToken t = tokens[i];
						
						Expr e = null;
						
						if(i >= stack.size()) {
							return false;
							// throw new IllegalStateException(String.format("Stack:%s, tokLen:%d, expSize:%d, sSize:%d, sHeight:%d, i:%d", stack, tokens.length, expectedSize, stack.size(), stack.height(), i));
						} else {
							e = stack.peek(i);
						}
						if(!t.matches(e)) {
							return false;
						}
					}
				}
			}
			
			return true;
		}
	}
	
	private static void compile_configs(String[] configs) {
		for(String c : configs) {
			String[] ps = c.split(":");
			
			int op = Integer.parseInt(ps[0]);
			int num_confs = Integer.parseInt(ps[1]);
			
			if(ps.length != (2 + num_confs) /*header fields + confs*/) {
				throw new UnsupportedOperationException("Cannot parse config: " + c);
			}
			
			List<VerifierRule> compiledConfs = new ArrayList<>();
			
			for(int i=0; i < num_confs; i++) {
				String conf = ps[2 + i];
				
				String[] cfs = conf.split(">");
				
				String pre, post;
				if(cfs.length == 0) {
					pre = "";
					post = "";
				} else if(cfs.length == 1) {
					// decide which side is empty
					if(conf.charAt(0) == '>') {
						// first is empty
						pre = "";
						post = cfs[0];
					} else {
						// second is empty
						pre = cfs[0];
						post = "";
					}
				} else if(cfs.length == 2) {
					pre = cfs[0];
					post = cfs[1];
				} else {
					throw new UnsupportedOperationException("Malformed config @" + i + ":: " + c);
				}
				
				try {
					VerifierToken[] preTokens = VerifierToken.makeTokens(pre);
					VerifierToken[] postTokens = VerifierToken.makeTokens(post);
					
					VerifierRule rule = new VerifierRule(preTokens, postTokens);
					compiledConfs.add(rule);
				} catch(Exception e) {
					throw new UnsupportedOperationException("Malformed config: " + conf + " in " + c, e);
				}
			}
			
			vrules.put(op, compiledConfs);
			__vrules.put(op, Printer.OPCODES[op] + "/" + c);
		}
	}
	
	private final ControlFlowGraphBuilder builder;
	
	public GenerationVerifier(ControlFlowGraphBuilder builder) {
		this.builder = builder;
	}
	
	List<VerifierRule> find_verify_matches() {
		throwNoContext();
		
		int op = currentContext.insn.getOpcode();
		ExpressionStack stack = currentContext.stack;
		
		List<VerifierRule> rules = vrules.get(op);
		
		if(rules == null) {
			System.err.println("Cannot verify " + Printer.OPCODES[op] + " (no rules).  Stack: " + stack);
			return null;
		}
		
		List<VerifierRule> possible = new ArrayList<>();
		
		for(VerifierRule r : rules) {
			if(r.match_attempt(stack, true)) {
				possible.add(r);
			}
		}
		
		if(possible.isEmpty() && !rules.isEmpty()) {
			throw new VerifyException(ExceptionStage.PRE, currentContext);
		}
		
		return possible;
	}
	
	void confirm_rules(List<VerifierRule> rules) {
		throwNoContext();
		
		int opcode = currentContext.insn.getOpcode();
		ExpressionStack stack = currentContext.stack;
		
		List<VerifierRule> vr = vrules.get(opcode);
		
		if(vr.size() > 0) {
			for(VerifierRule r : rules) {
				if(r.match_attempt(stack, false)) {
					return;
				}
			}
			
			throw new VerifyException(ExceptionStage.POST, currentContext);
		}
	}
	
	private void throwNoContext() {
		if(currentContext == null) {
			throw new IllegalStateException("No context");
		}
	}
	
	public GenerationContext newContext(ExpressionStack stack, AbstractInsnNode insn, BasicBlock block) {
		GenerationContext cxt = new GenerationContext(stack, insn, block);
		currentContext = cxt;
		return cxt;
	}
	
	public void addEvent(GenerationEvent e) {
		currentContext.events.add(e);
	}
	
	private GenerationContext currentContext;
	private static final Map<Integer, String> __vrules;
	private static final Map<Integer, List<VerifierRule>> vrules;
	
	static {
		__vrules = new HashMap<>();
		vrules = new HashMap<>();
		compile_configs(STACK_CONFIGS);
	}
	
	public static interface GenerationEvent {
	}
	
	public static class GenerationMessageEvent implements GenerationEvent {
		private final String msg;
		
		public GenerationMessageEvent(String msg) {
			this.msg = msg;
		}
		
		@Override
		public String toString() {
			return msg;
		}
	}
	
	public static class GenerationContext {
		public final ExpressionStack stack;
		public final AbstractInsnNode insn;
		public final BasicBlock block;
		public final List<GenerationEvent> events;
		
		public GenerationContext(ExpressionStack stack, AbstractInsnNode insn, BasicBlock block) {
			this.stack = stack;
			this.insn = insn;
			this.block = block;
			events = new ArrayList<>();
		}
		
		@Override
		public String toString() {
			StringBuilder msglog = new StringBuilder();
			for(GenerationEvent e : events) {
				msglog.append(e).append(System.lineSeparator());
			}
			return String.format("%s, b: #%s, stack: %s%n  Eventlog(%d):%s", Printer.OPCODES[insn.getOpcode()].toLowerCase(), block.getDisplayName(), stack, events.size(), msglog);
		}
	}
	
	public enum ExceptionStage {
		PRE, POST
	}
	
	public class VerifyException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		
		private final ExceptionStage stage;
		private final GenerationContext context;
		
		public VerifyException(ExceptionStage stage, GenerationContext context) {
			super(String.format("error during %s / %s for %s%n%s", builder.method, stage, __vrules.get(context.insn.getOpcode()), context));
			this.stage = stage;
			this.context = context;
		}
		
		public ExceptionStage getStage() {
			return stage;
		}
		
		public GenerationContext getContext() {
			return context;
		}
	}
}
