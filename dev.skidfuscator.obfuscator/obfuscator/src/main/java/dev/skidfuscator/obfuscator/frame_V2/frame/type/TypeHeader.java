package dev.skidfuscator.obfuscator.frame_V2.frame.type;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * WIP
 */
public class TypeHeader {
    /**
     * Set denoting of all the parent type headers
     */
    private final Set<TypeHeader> parents;

    /**
     * Warning: Can be null
     */
    private final Type[] types;

    /**
     * Set denoting of all the excluded types for the computation
     */
    private final Set<Type>[] excluded;

    public TypeHeader(Type[] types) {
        this(types, new HashSet<>(), new HashSet[types.length]);
    }

    public TypeHeader(Type[] types, Set<TypeHeader> parents, Set<Type>[] excluded) {
        this.parents = parents;
        this.types = types;
        this.excluded = excluded;
    }

    public Set<Type> get(final int index) {
        return _get(index, new HashSet<>());
    }

    public void set(final int index, final Type type) {
        this.types[index] = type;
    }

    public void addParent(final TypeHeader typeHeader) {
        parents.add(typeHeader);
    }

    public void addExclusion(final int index, final Type type) {
        if (excluded[index] == null) {
            excluded[index] = new HashSet<>();
        }
        excluded[index].add(type);
    }

    private Set<Type> _get(final int index, final Set<TypeHeader> visited) {
        visited.add(this);

        final Set<Type> excluded = this.excluded[index];
        final Type type = this.types[index];

        /*
         * Root node. There are a couple of essential requirements for this
         * node:
         * 
         * 1) It must not be a null type
         * 2) It must not be exempted by its own exclusions
         */
        if (parents.isEmpty()) {
            if (type == null) {
                throw new IllegalStateException("Root type is denoted as null!");
            }
            
            if (excluded != null && excluded.contains(type)) {
                throw new IllegalStateException("Root type excluded by root composition");
            }
            
            return new HashSet<>(Collections.singleton(type));
        }
        
        /*if (depth >= maxDepth) {
            return null;
        }

        for (ExpressionPool parent : parents) {
            if ((type = parent.get(index, predicate, depth + 1, maxDepth)) == null)
                continue;

            return type;
        */

        final Set<Type> types = new HashSet<>();

        if (type != null && (excluded == null || !excluded.contains(type))) {
            return new HashSet<>(Collections.singleton(type));
        }

        for (TypeHeader parent : parents) {
            if (visited.contains(parent))
                continue;
            
            final Set<Type> parentTypes = parent._get(index, visited);

            for (Type parentType : parentTypes) {
                if (excluded != null && excluded.contains(parentType))
                    continue;

                types.add(parentType);
            }
        }

        if (types.isEmpty()) {
            types.add(TypeUtil.UNDEFINED_TYPE);
        }

        return types;
    }

    public void fill(final Type type) {
        Arrays.fill(types, type);
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

    public Type[] getAppend() {
        return types;
    }

    public Set<Type>[] getExcluded() {
        return excluded;
    }

    public TypeHeader createChild() {
        final TypeHeader typeHeader = new TypeHeader(new Type[this.types.length]);
        typeHeader.addParent(this);
        return typeHeader;
    }

    public TypeHeader createChild(final int index, final Type type) {
        final TypeHeader typeHeader = new TypeHeader(new Type[this.types.length]);
        typeHeader.set(index, type);
        typeHeader.addParent(this);
        return typeHeader;
    }
}
