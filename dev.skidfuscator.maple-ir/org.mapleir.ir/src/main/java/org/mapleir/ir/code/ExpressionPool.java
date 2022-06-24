package org.mapleir.ir.code;

import org.mapleir.ir.TypeUtils;
import org.objectweb.asm.Type;

import javax.lang.model.util.Types;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExpressionPool implements Iterable<Type> {
    protected final Set<ExpressionPool> parents;
    protected Type[] types;
    protected final Set<Type> excludedTypes;

    public ExpressionPool(ExpressionPool parent) {
        this.parents = new HashSet<>(Collections.singleton(parent));
        this.types = new Type[parent.size()];
        this.excludedTypes = new HashSet<>(parent.excludedTypes);
    }

    public ExpressionPool(Type[] types) {
        this.parents = new HashSet<>();
        this.types = types;
        this.excludedTypes = new HashSet<>();
    }

    protected ExpressionPool(Set<ExpressionPool> parent, Type[] types) {
        this.parents = parent;
        this.types = types;
        this.excludedTypes = parent
                .stream()
                .flatMap(e -> e.getExcludedTypes().stream())
                .collect(Collectors.toSet());
    }

    public void set(final int index, final Type type) {
        assert index < types.length : "Provided index is larger than allocated array! " +
                "(" + index + " / " + types.length + ")";

        final boolean weirdtype = type.equals(Type.LONG_TYPE) || type.equals(Type.DOUBLE_TYPE);

        assert !weirdtype || index + 1 < types.length : "Provided expanded index is larger than allocated pool size!";

        types[index] = type;

        if (weirdtype) {
            types[index + 1] = Type.VOID_TYPE;
        }
    }

    public Type get(final int index) {
        return get(index, null);
    }
    public Type get(final int index, final Predicate<Type> predicate) {
        Type type = types[index];

        if (predicate != null && predicate.test(type) && parents.isEmpty()) {
            return null;
        }

        if ((type = types[index]) == null) {
            int i = 0;
            while (true) {
                for (ExpressionPool parent : parents) {
                    type = parent.get(index, predicate, 0, i);
                    if ((type) == null || (predicate != null && predicate.test(type)))
                        continue;

                    return type;
                }

                if (i > 100)
                    break;

                i++;
            }

            throw new IllegalStateException("Failed to compute top parent with candidate");
        }

        return type;
    }

    protected Type get(final int index, final Predicate<Type> predicate, final int depth, final int maxDepth) {
        Type type;

        if ((type = types[index]) == null) {
            if (depth >= maxDepth) {
                return null;
            }

            for (ExpressionPool parent : parents) {
                if ((type = parent.get(index, predicate, depth + 1, maxDepth)) == null)
                    continue;

                return type;
            }

            return null;
        }

        return type;
    }

    public void fill(final Type type) {
        Arrays.fill(types, type);
    }

    public void copy(final ExpressionPool expressionPool) {
        for (int i = 0; i < expressionPool.size(); i++) {
            types[i] = expressionPool.getTypes()[i];
        }
    }

    public void addParent(final ExpressionPool expressionPool) {
        parents.add(expressionPool);
    }

    public void addExclusion(final Type type) {
        excludedTypes.add(type);
    }

    public Type[] getTypes() {
        return types;
    }

    public Type[] getComputedTypes() {
        final Type[] type = new Type[types.length];

        for (int i = 0; i < types.length; i++) {
            type[i] = get(i);
        }

        return type;
    }

    public Set<ExpressionPool> getParents() {
        return parents;
    }

    public int size() {
        return types.length;
    }

    public void setTypes(Type[] types) {
        this.types = types;
    }

    public int computeSize() {
        int lastExist = 0;

        for (int i = size(); i > 0; i--) {
            final Type grab = get(i - 1);
            final boolean skip = grab.equals(Type.VOID_TYPE) || grab.equals(TypeUtils.UNDEFINED_TYPE);
            if (!skip) {
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

    protected ExpressionPool _self() {
        return new ExpressionPool(this);
    }

    public Set<Type> getExcludedTypes() {
        return excludedTypes;
    }

    @Override
    public Iterator<Type> iterator() {
        return Arrays.stream(types).iterator();
    }

    @Override
    public void forEach(Consumer<? super Type> action) {
        Arrays.stream(types).forEach(action);
    }

    @Override
    public Spliterator<Type> spliterator() {
        return  Arrays.stream(types).spliterator();
    }

    public ExpressionPool copy() {
        return new ExpressionPool(new HashSet<>(parents), Arrays.copyOf(types, types.length));
    }
}
