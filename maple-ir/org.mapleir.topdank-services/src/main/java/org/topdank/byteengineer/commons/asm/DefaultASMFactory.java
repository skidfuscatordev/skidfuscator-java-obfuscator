package org.topdank.byteengineer.commons.asm;

import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;

public class DefaultASMFactory implements ASMFactory<ClassNode> {

	@Override
	public ClassNode create(byte[] bytes, String name) {
		return ClassHelper.create(bytes);
	}

	@Override
	public byte[] write(ClassNode c) {
		return ClassHelper.toByteArray(c);
	}
}
