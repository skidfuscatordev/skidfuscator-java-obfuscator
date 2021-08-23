package dev.skidfuscator.obf.transform_legacy.parameter;

import dev.skidfuscator.obf.asm.MethodWrapper;

public interface ParameterResolver {
    MethodWrapper getWrapper(final String owner, final String name, final String desc);
}
