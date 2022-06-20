package org.topdank.banalysis.asm.desc;

/**
 * Primitive desc type enum.
 * @author Bibl
 * 
 */
public enum Primitive {
	
	/** Integer type (I) **/
	INT("I"),
	/** Short type (S) **/
	SHORT("S"),
	/** Byte type(B) **/
	BYTE("B"),
	/** Float type (F) **/
	FLOAT("F"),
	/** Double type (D) **/
	DOUBLE("D"),
	/** Long type (J) **/
	LONG("J"),
	/** Boolean type (Z) **/
	BOOLEAN("Z");
	
	private String desc;
	
	Primitive(String desc) {
		this.desc = desc;
	}
	
	/**
	 * @return Internal bytecode description type representation.
	 */
	public String desc() {
		return desc;
	}
	
	/**
	 * Converts a bytecode descriptor to the equivilient primitive type.
	 * @param desc Bytecode descriptor.
	 * @return {@link Primitive} or null if the desc is an array or a reference type.
	 */
	public static Primitive translate(String desc) {
		for(Primitive primitive : Primitive.values()) {
			if (primitive.desc.equals(desc))
				return primitive;
		}
		return null;
	}
}