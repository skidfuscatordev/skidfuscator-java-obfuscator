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
            if (predicate != null && predicate.test(currentType) && parents.isEmpty()) {
                System.out.println(
                        "Failed to match predicate: " + currentType
                        +"\n Excluded: " + Arrays.toString(excludedTypes.toArray())
                );
                return null;
            }

            return currentType;
        }

        for (ExpressionPool parent : parents) {
            if (visited.contains(parent))
                continue;

            Type found = ((SkidExpressionPool) parent).get(index, predicate, visited);

            if (predicate != null && predicate.test(found))
                continue;

            currentType = TypeUtil.mergeTypes(
                    skidfuscator,
                    found,
                    currentType
            );
        }

        return currentType;
    }

    public boolean isConflicting() {
        return conflict.values().stream().anyMatch(e -> e);
    }

    @Override
    public SkidExpressionPool copy() {
        return new SkidExpressionPool(new HashSet<>(parents), Arrays.copyOf(types, types.length), skidfuscator);
    }
}
