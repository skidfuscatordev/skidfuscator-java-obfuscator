package dev.skidfuscator.obf.transform_legacy.parameter;

import dev.skidfuscator.obf.init.SkidSession;

public interface ParameterTransformer {
    ParameterResolver transform(final SkidSession session);
}
