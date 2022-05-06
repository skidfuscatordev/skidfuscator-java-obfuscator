package dev.skidfuscator.obf.exclusion;

import java.util.EnumMap;

public class ExclusionMap extends EnumMap<ExclusionType, ExclusionTester<?>> {
    public ExclusionMap() {
        super(ExclusionType.class);
    }

    public <T> ExclusionTester<T> poll(Object key) {
        return (ExclusionTester<T>) super.get(key);
    }
}
