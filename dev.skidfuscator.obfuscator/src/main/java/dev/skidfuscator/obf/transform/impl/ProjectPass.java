package dev.skidfuscator.obf.transform.impl;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidMethod;

public interface ProjectPass {
    void pass(final SkidSession session);

    String getName();
}
