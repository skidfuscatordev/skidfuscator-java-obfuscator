package org.mapleir.asm;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.util.IHasJavaDesc;
import org.mapleir.stdlib.util.JavaDesc;
import org.objectweb.asm.Opcodes;

public class FieldNode implements FastGraphVertex, IHasJavaDesc {
    private static int ID_COUNTER = 1;
   	private final int numericId = ID_COUNTER++;

   	public final ClassNode owner;
    public final org.objectweb.asm.tree.FieldNode node;

    public FieldNode(org.objectweb.asm.tree.FieldNode node, ClassNode owner) {
        this.node = node;
        this.owner = owner;
    }

    @Override
    public String getDisplayName() {
        return node.name;
    }

    @Override
    public int getNumericId() {
        return numericId;
    }

    @Override
    public String getOwner() {
        return owner.getName();
    }

    @Override
    public String getName() {
        return node.name;
    }

    @Override
    public String getDesc() {
        return node.desc;
    }

    @Override
    public JavaDesc.DescType getDescType() {
        return JavaDesc.DescType.FIELD;
    }

    @Override
    public JavaDesc getJavaDesc() {
	    return new JavaDesc(owner.getName(), getName(), getDesc(), JavaDesc.DescType.FIELD);
    }

    public boolean isStatic() {
        return (node.access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isVolatile() {
        return (node.access & Opcodes.ACC_VOLATILE) != 0;
    }

    public boolean isTransient() {
        return (node.access & Opcodes.ACC_TRANSIENT) != 0;
    }

    public boolean isFinal() {
        return (node.access & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isSynthetic() {
        return (node.access & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isPublic() {
        return (node.access & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isProtected() {
        return (node.access & Opcodes.ACC_PROTECTED) != 0;
    }

    public boolean isPrivate() {
        return (node.access & Opcodes.ACC_PRIVATE) != 0;
    }
}
