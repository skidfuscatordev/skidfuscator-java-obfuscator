package org.mapleir.ir.code;

/**
 * An exception which indicates that data is being read or written at an index
 * that is not used by a given type of node.
 */
public class ChildOutOfBoundsException extends IndexOutOfBoundsException {

	public ChildOutOfBoundsException(CodeUnit parent, int index) {
		super(String.format("index=%d in <%s>", index, parent.getDisplayName()));
	}
}
