package dev.skidfuscator.obf.transform.flow;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform.yggdrasil.SkidMethod;

public interface FlowPass {
    void pass(final SkidSession session, final SkidMethod method);
}
