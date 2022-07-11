package org.mapleir.ir.locals.dynamic;

import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * WIP
 */
public class DynamicLocalHeader {
    /**
     * Set denoting of all the parent type headers
     */
    private final Set<DynamicLocalHeader> parents;

    /**
     * Warning: Can be null
     */
    private final DynamicLocalsPool.LocalCoverage[] types;

    public DynamicLocalHeader(DynamicLocalsPool.LocalCoverage[] types) {
        this(types, new HashSet<>());
    }

    public DynamicLocalHeader(DynamicLocalsPool.LocalCoverage[] types, Set<DynamicLocalHeader> parents) {
        this.parents = parents;
        this.types = types;
    }

    public Set<DynamicLocalsPool.LocalCoverage> get(final int index) {
        return _get(index, new HashSet<>());
    }

    public void set(final int index, final DynamicLocalsPool.LocalCoverage type) {
        this.types[index] = type;
    }

    public void addParent(final DynamicLocalHeader typeHeader) {
        parents.add(typeHeader);
    }

    private Set<DynamicLocalsPool.LocalCoverage> _get(final int index, final Set<DynamicLocalHeader> visited) {
        visited.add(this);

        final DynamicLocalsPool.LocalCoverage type = this.types[index];

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

        final Set<DynamicLocalsPool.LocalCoverage> types = new HashSet<>();

        for (DynamicLocalHeader parent : parents) {
            if (visited.contains(parent))
                continue;
            
            final Set<DynamicLocalsPool.LocalCoverage> parentTypes = parent._get(index, visited);
            types.addAll(parentTypes);
        }

        return types;
    }

    public void fill(final Type type) {
        Arrays.fill(types, type);
    }

    public int size() {
        return types.length;
    }

    public DynamicLocalsPool.LocalCoverage[] getAppend() {
        return types;
    }

    public DynamicLocalHeader createChild() {
        final DynamicLocalHeader typeHeader = new DynamicLocalHeader(new DynamicLocalsPool.LocalCoverage[this.types.length]);
        typeHeader.addParent(this);
        return typeHeader;
    }

    public DynamicLocalHeader createChild(final int index, final DynamicLocalsPool.LocalCoverage type) {
        final DynamicLocalHeader typeHeader = new DynamicLocalHeader(new DynamicLocalsPool.LocalCoverage[this.types.length]);
        typeHeader.set(index, type);
        typeHeader.addParent(this);
        return typeHeader;
    }
}
