package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.code.ExpressionPool;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SkidExpressionPool extends ExpressionPool {
    private final Skidfuscator skidfuscator;

    public SkidExpressionPool(ExpressionPool parent, Skidfuscator skidfuscator) {
        super(parent);
        this.skidfuscator = skidfuscator;
    }

    public SkidExpressionPool(Type[] types, Skidfuscator skidfuscator) {
        super(types);
        this.skidfuscator = skidfuscator;
    }

    private SkidExpressionPool(Set<ExpressionPool> parent, Type[] types, Skidfuscator skidfuscator) {
        super(parent, types);
        this.skidfuscator = skidfuscator;
    }

    public int computeSize() {
        int lastExist = 0;

        for (int i = size(); i > 0; i--) {
            if (!get(i - 1).equals(Type.VOID_TYPE)) {
                lastExist = i;
                break;
            }

            if (i == 1) {
                continue;
            }

            final Type subType = get(i - 2);
            if (subType.equals(Type.DOUBLE_TYPE) || subType.equals(Type.LONG_TYPE)) {
                lastExist = i;
                break;
            }
        }
        return lastExist;
    }

    @Override
    public void merge(ExpressionPool other) {
        if (other.getTypes().length >= this.types.length) {
            Type[] s = new Type[other.getTypes().length];
            System.arraycopy(types, 0, s, 0, types.length);
            types = s;
        }

        for (int i = 0; i < other.getTypes().length; i++) {
            final Type selfType = this.types[i];
            final Type otherType = other.getTypes()[i];

            final boolean selfFilled = selfType != Type.VOID_TYPE && selfType != null;
            final boolean otherFilled = otherType != Type.VOID_TYPE && otherType != null;

            if (selfFilled && otherFilled && selfType != otherType) {
                final ClassNode selfClassNode = skidfuscator.getClassSource().findClassNode(selfType.getInternalName());
                final ClassNode otherClassNode = skidfuscator.getClassSource().findClassNode(otherType.getInternalName());

                final ClassNode commonClassNode = skidfuscator.getClassSource()
                        .getClassTree()
                        .getCommonAncestor(Arrays.asList(selfClassNode, otherClassNode))
                        .iterator()
                        .next();

                this.types[i] = Type.getType("L" + commonClassNode.getName() + ";");
                continue;
                //throw new IllegalStateException("Trying to merge " + selfType
                //        + " (self) with " + otherType + " (other) [FAILED] [" + i + "]");
            }

            if (otherFilled && !selfFilled) {
                this.types[i] = otherType;
            }
        }
    }

    @Override
    public SkidExpressionPool copy() {
        return new SkidExpressionPool(new HashSet<>(parents), Arrays.copyOf(types, types.length), skidfuscator);
    }
}
