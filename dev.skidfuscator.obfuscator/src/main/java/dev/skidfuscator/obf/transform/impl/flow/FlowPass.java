package dev.skidfuscator.obf.transform.impl.flow;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidMethod;

public interface FlowPass {
    void pass(final SkidSession session, final SkidMethod method);
}
