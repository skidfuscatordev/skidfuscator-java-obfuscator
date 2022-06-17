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

    @Override
    public Type get(int index) {
        return super.get(index);
    }

    @Override
    public SkidExpressionPool copy() {
        return new SkidExpressionPool(new HashSet<>(parents), Arrays.copyOf(types, types.length), skidfuscator);
    }
}
