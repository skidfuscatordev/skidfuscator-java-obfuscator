package dev.skidfuscator.obfuscator.exempt;

import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import java.util.EnumMap;

public class ExclusionMap extends EnumMap<ExclusionType, ExclusionTester<?>> {
    public ExclusionMap() {
        super(ExclusionType.class);

        this.put(ExclusionType.CLASS, new ExclusionTester<ClassNode>() {
            @Override
            public boolean test(ClassNode var) {
                return false;
            }

            @Override
            public String toString() {
                return "ExclusionTester={DefaultExemptTester}";
            }
        });
        this.put(ExclusionType.METHOD, new ExclusionTester<MethodNode>() {
            @Override
            public boolean test(MethodNode var) {
                return false;
            }

            @Override
            public String toString() {
                return "ExclusionTester={DefaultExemptTester}";
            }
        });
    }

    public <T> ExclusionTester<T> poll(Object key) {
        return (ExclusionTester<T>) super.get(key);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}