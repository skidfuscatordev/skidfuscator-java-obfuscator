package dev.skidfuscator.obf.transform.type;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidGraph;

public interface SkidGraphTransformer {
    void run(final SkidSession skidSession, final SkidGraph graph);
}
