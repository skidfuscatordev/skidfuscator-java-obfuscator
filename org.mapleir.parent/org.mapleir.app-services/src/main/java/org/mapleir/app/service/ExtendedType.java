package org.mapleir.app.service;

import java.util.LinkedList;

import org.objectweb.asm.Type;
import org.mapleir.asm.ClassNode;

public class ExtendedType {

	public enum Sort {
		MONO, CONE
	}
	
	private final Type type;
	private final Sort sort;
	private final int hashCode;
	
	public ExtendedType(Type type, Sort sort) {
		if(type.getSort() != Type.OBJECT) {
			throw new UnsupportedOperationException(String.format("Cannot form type cone from %s", type));
		}
		
		this.type = type;
		this.sort = sort;
		hashCode = ((31 * type.hashCode()) + sort.hashCode());
	}
	
	/**
	 * Checks whether this extended type is included into the set of types
	 * represented by the other extended type.
	 * @param source
	 * @param other
	 * @return
	 */
	public boolean isIncluded(ApplicationClassSource source, ExtendedType other) {
		if(other == null) {
			return false;
		} else if(this.equals(other)) {
			return true;
		}
		
		/* if the other one is MONO:
		 *   and we are MONO, we checked with equals above so
		 *   they can't be included
		 *   
		 *   if we are CONE, we cannot be included in a MONO(other)
		 */
		
		if(other.getSort() == Sort.MONO) {
			return false;
		}
		
		/* other is CONE, we need to check if our
		 * type is included in the other's type cone*/
		
		ClassNode klass = findNode(source, type);
		LinkedList<ClassNode> wl = new LinkedList<>();
		wl.addLast(klass);
		
		while(!wl.isEmpty()) {
			ClassNode cur = wl.removeFirst();
			
			ClassNode parent = source.findClassNode(cur.node.superName);
			if(parent != null) {
				if(parent.getName().equals(type.getInternalName())) {
					return true;
				}
				
				wl.addLast(parent);
			}
			
			for(String siface : cur.node.interfaces) {
				if(siface.equals(type.getInternalName())) {
					return true;
				}
				
				wl.addLast(source.findClassNode(siface));
			}
		}
		
		return false;
	}
	
	private ClassNode findNode(ApplicationClassSource source, Type t) {
		if(t.getSort() != Type.OBJECT) {
			throw new UnsupportedOperationException(String.format("Cannot find class for %s", t));
		}
		
		return source.findClassNode(t.getInternalName());
	}
	
	public Type getBase() {
		return type;
	}
	
	public Sort getSort() {
		return sort;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExtendedType other = (ExtendedType) obj;
		if (sort != other.sort)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("<%s, %s>", type, sort);
	}
}
