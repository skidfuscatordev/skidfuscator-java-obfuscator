package org.topdank.byteengineer.commons.data;

import org.mapleir.asm.ClassNode;

import java.util.Arrays;

public final class JarClassData {

	private final String name;
	private final byte[] data;
	private final ClassNode classNode;

	public JarClassData(String name, byte[] data, ClassNode classNode) {
		this.name = name;
		this.data = data;
		this.classNode = classNode;
	}

	public String getName() {
		return name;
	}
	
	public byte[] getData() {
		return data;
	}

	public ClassNode getClassNode() {
		return classNode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + Arrays.hashCode(data);
		result = (prime * result) + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JarClassData other = (JarClassData) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}