package org.mapleir.ir.locals.type;

import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Set;

public class TypePool {
    private final TypeHeader[] types;

    public TypePool(TypeHeader[] types) {
        this.types = types;
    }

    public Set<Type> get(final int index) {
        return types[index].get();
    }

    public void set(final int index, Type type) {
        types[index].set(type);
    }

    public int computeSize() {
        for (int i = size(); i > 0; i--) {
            final Set<Type> computed = this.get(i);

            /*
             * If the computed types are empty, it means there's
             * no defined type at this index; Meaning, it has to
             * be either a dead local end or something.
             */
            if (computed.isEmpty()) {
                continue;
            }

            if (i > 1) {
                final Set<Type> subType = this.get(i - 2);

                if (subType.contains(Type.DOUBLE_TYPE) || subType.contains(Type.LONG_TYPE)) {
                    return i;
                }
            }

            for (Type type : computed) {
                if (type.equals(Type.VOID_TYPE)) {
                    continue;
                }

                return i;
            }
        }
        return 0;
    }

    public int size() {
        return types.length;
    }

    public TypePool createChild(int index, Type header) {
        return createChild(new int[]{index}, new Type[]{header});
    }

    public TypePool createChild(int[] indexes, Type[] headers) {
        assert indexes.length == headers.length : "No changes matched";

        TypeHeader[] array = Arrays.copyOf(types, types.length);

        for (int i = 0; i < indexes.length; i++) {
            final int index = indexes[i];
            final TypeHeader header = new TypeHeader(headers[i]);

            header.addParent(array[index]);
            array[index] = header;
        }

        return new TypePool(array);
    }
}
