package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.code.TypeStack;
import org.objectweb.asm.Type;

import java.util.Arrays;

public class SkidTypeStack extends TypeStack {
    private final Skidfuscator skidfuscator;

    public SkidTypeStack(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }

    public SkidTypeStack(int capacity, Skidfuscator skidfuscator) {
        super(capacity);
        this.skidfuscator = skidfuscator;
    }

    @Override
    public void merge(TypeStack other) {
        if (other.getStack().length >= this.getStack().length) {
            Type[] s = new Type[other.capacity()];
            System.arraycopy(this.getStack(), 0, s, 0, this.capacity());
            stack = s;
        }

        for (int i = 0; i < other.capacity(); i++) {
            final Type selfType = this.stack[i];
            final Type otherType = other.getStack()[i];

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


                this.stack[i] = Type.getType("L" + commonClassNode.getName() + ";");
                continue;
                //throw new IllegalStateException("Trying to merge " + selfType
                //        + " (self) with " + otherType + " (other) [FAILED] [" + i + "]");
            }

            if (otherFilled && !selfFilled) {
                this.stack[i] = otherType;
            }
        }
    }

    public int computeSize() {
        int lastExist = 0;

        for (int i = size(); i > 0; i--) {
            if (!stack[i - 1].equals(Type.VOID_TYPE)) {
                lastExist = i;
                break;
            }

            if (i == 1) {
                continue;
            }

            final Type subType = stack[i - 2];
            if (subType.equals(Type.DOUBLE_TYPE) || subType.equals(Type.LONG_TYPE)) {
                lastExist = i;
                break;
            }
        }

        return lastExist;
    }

    @Override
    public SkidTypeStack copy() {
        SkidTypeStack stack = new SkidTypeStack(size(), skidfuscator);
        copyInto(stack);
        return stack;
    }
}
