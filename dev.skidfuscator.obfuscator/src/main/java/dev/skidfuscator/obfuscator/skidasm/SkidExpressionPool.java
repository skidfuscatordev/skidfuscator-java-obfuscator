package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.code.ExpressionPool;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SkidExpressionPool extends ExpressionPool {
    private final Skidfuscator skidfuscator;

    private Map<Integer, Boolean> conflict = new HashMap<>();

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

    @Override
    public Type get(int index) {
        return get(index, new Predicate<Type>() {
            @Override
            public boolean test(Type type) {
                return excludedTypes.contains(type);
            }
        });
    }

    @Override
    public Type get(int index, Predicate<Type> nullifier) {
        return get(index, nullifier, new HashSet<>());
    }

    public Type get(int index, Predicate<Type> predicate, Set<ExpressionPool> visited) {
        visited.add(this);

        Type currentType = types[index];

        if (currentType != null) {
            /*
             * If we have any sort of exemptions, and this is a root
             * expression pool (no parents), and the type corresponds
             * to the predicate
             *
             * --> then return null (and warn!)
             *
             * TODO: fix the parent thing being wrong (should iterate only
             *      if the parents is null, not all scenarios)
             */
            if (parents.isEmpty() && predicate != null && predicate.test(currentType)) {
                System.out.println(
                        "Failed to match predicate: " + currentType
                        +"\n Excluded: " + Arrays.toString(excludedTypes.toArray())
                );
                return null;
            }

            /*
             * cool, here we have computed a type which isn't excised
             * and which is usable (not null)
             */
            return currentType;
        }

        /*
         * No cache for the type in this pool. Okay. lets iterate through
         * each parent (whilst making sure we don't double iterate by
         * using a visitor set).
         *
         * If the type is found, check if it isn't the *spooky banned type*.
         * If it is, then skip it and look for another in other parents
         */
        for (ExpressionPool parent : parents) {
            if (visited.contains(parent))
                continue;

            Type found = ((SkidExpressionPool) parent).get(index, predicate, visited);

            if (predicate != null && predicate.test(found))
                continue;

            /* Merge the types */
            currentType = TypeUtil.mergeTypes(
                    skidfuscator,
                    found,
                    currentType
            );
        }

        return currentType;
    }

    public Set<Type> getTypes(int index) {
        return getTypes(index, new HashSet<>(), new HashSet<>());
    }

    protected Set<Type> getTypes(int index, Set<Type> computedTypes, Set<ExpressionPool> visited) {
        visited.add(this);

        final Type currentType = types[index];

        if (currentType != null) {
            computedTypes.add(currentType);
        }

        for (ExpressionPool parent : parents) {
            if (visited.contains(parent))
                continue;

            computedTypes = ((SkidExpressionPool) parent).getTypes(index, computedTypes, visited);
        }

        return computedTypes;
    }

    @Deprecated
    public boolean isConflicting() {
        return conflict.values().stream().anyMatch(e -> e);
    }

    @Override
    public SkidExpressionPool copy() {
        return new SkidExpressionPool(new HashSet<>(parents), Arrays.copyOf(types, types.length), skidfuscator);
    }
}
