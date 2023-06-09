package org.mapleir.ir.code;

public interface Opcode {

	String[][] OPNAMES = new String[][] {
			/*EMPTY_CLASS_0*/{
				
			},
			/*CLASS_STORE*/{
				"v_store",
				"arr_store",
				"f_store",
				"phi_store"
			},
			/*CLASS_LOAD*/{
				"v_load",
				"arr_load",
				"f_load",
				"c_load"
			},
			/*CLASS_FUNC*/{
				"invoke",
				"dinvoke",
				"consume",
				"return"
			},
			/*CLASS_ARITHMETIC*/{
				"arith",
				"neg"
			},
			/*CLASS_JUMP*/{
				"cond",
				"uncond",
				"switch"
			},
			/*CLASS_OBJ*/{
				"uninit",
				"init",
				"narray"
			},
			/*CLASS_INTERN*/{
				"alen",
				"cast",
				"inst",
				"cmp",
				"catch",
				"throw",
				"monitor"
			},
			/*EMPTY_CLASS_8*/{
				
			},
			/*CLASS_PHI*/{
				"phi",
				"ephi"
			}
	};
	
	static String opname(int op) {
		byte b1 = (byte) ((op >> 8) & 0xFF);
		byte b2 = (byte) ((op >> 0) & 0xFF);
		
		if(b1 < 0 || b1 >= OPNAMES.length) {
			throw new IllegalStateException("No class: 0x" + Integer.toHexString(op));
		} else if(b2 <= 0 || b2 > OPNAMES[b1].length) {
			throw new IllegalStateException("No opcode: 0x" + Integer.toHexString(op) + (b2 == 0 ? " (class)" : ""));
		}
		return OPNAMES[b1][b2 - 1];
	}
	
	static int opclass(int op) {
		return ((op >> 8) & 0xFF) << 8;
	}
	
	int CLASS_STORE    = 0x100;
	int LOCAL_STORE    = 0x101;
	int ARRAY_STORE    = 0x102;
	int FIELD_STORE    = 0x103;
	int PHI_STORE      = 0x104;
	
	int CLASS_LOAD     = 0x200;
	int LOCAL_LOAD     = 0x201;
	int ARRAY_LOAD     = 0x202;
	int FIELD_LOAD     = 0x203;
	int CONST_LOAD     = 0x204;
	
	int INVOKE         = 0x301;
	int POP            = 0x302;
	int RETURN         = 0x303;
	
	int ARITHMETIC     = 0x401;
	int NEGATE         = 0x402;
	
	int CLASS_JUMP     = 0x500;
	int COND_JUMP      = 0x501;
	int UNCOND_JUMP    = 0x502;
	int SWITCH_JUMP    = 0x503;
	
	int CLASS_OBJ      = 0x600;
	int ALLOC_OBJ     = 0x601;
	int INIT_OBJ       = 0x602;
	int NEW_ARRAY      = 0x603;
	
	int ARRAY_LEN      = 0x701;
	int CAST           = 0x702;
	int INSTANCEOF     = 0x703;
	int COMPARE        = 0x704;
	int CATCH          = 0x705;
	int THROW          = 0x706;
	int MONITOR        = 0x707;
	
	int NOP            = 0x800;

	int CLASS_PHI      = 0x900;
	int PHI            = 0x901;
	int EPHI           = 0x902;

	int FRAME = 0x1000;
	int LINE_NO = 0x1001;
	int CLASS_RESERVED = 0x2000; // reserved for inner classes
}
