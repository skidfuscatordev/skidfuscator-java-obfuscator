package org.mapleir.ir.locals.type;

import org.mapleir.ir.code.ExpressionPool;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

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
    private Type type;

    /**
     * Set denoting of all the excluded types for the computation
     */
    private final Set<Type> excluded;

    public TypeHeader() {
        this(null);
    }

    public TypeHeader(Type type) {
        this.type = type;
        this.parents = new HashSet<>();
        this.excluded = new HashSet<>();
    }

    public Set<Type> get() {
        return _get(new HashSet<>());
    }

    public void set(final Type type) {
        this.type = type;
    }

    public void addParent(final TypeHeader typeHeader) {
        parents.add(typeHeader);
    }

    public void addExclusion(final Type type) {
        excluded.add(type);
    }

    private Set<Type> _get(final Set<TypeHeader> visited) {
        visited.add(this);

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
            
            if (excluded.contains(type)) {
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

        if (type != null && !excluded.contains(type)) {
            types.add(type);
        }

        for (TypeHeader parent : parents) {
            if (visited.contains(parent))
                continue;
            
            final Set<Type> parentTypes = parent._get(visited);

            for (Type parentType : parentTypes) {
                if (excluded.contains(parentType))
                    continue;

                types.add(parentType);
            }
        }

        return types;
    }
}
