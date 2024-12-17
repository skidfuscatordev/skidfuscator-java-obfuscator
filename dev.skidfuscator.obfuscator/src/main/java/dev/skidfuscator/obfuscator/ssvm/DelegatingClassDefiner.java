package dev.skidfuscator.obfuscator.ssvm;

import dev.xdark.ssvm.classloading.ClassDefiner;
import dev.xdark.ssvm.classloading.ClassDefinitionOption;
import dev.xdark.ssvm.classloading.ClassLoaderData;
import dev.xdark.ssvm.classloading.ParsedClassData;
import dev.xdark.ssvm.mirror.type.InstanceClass;

/**
 * Delegating class definer implementation to allow intercepting class definition.
 */
public class DelegatingClassDefiner implements ClassDefiner {
    private final ClassDefiner delegate;

    public DelegatingClassDefiner(ClassDefiner delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate cannot be null");
        }
        this.delegate = delegate;
    }

    @Override
    public ParsedClassData parseClass(String name, byte[] bytes, int off, int len, String source) {
        return delegate.parseClass(name, bytes, off, len, source);
    }

    protected ClassDefiner getDelegate() {
        return delegate;
    }
}