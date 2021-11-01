package dev.skidfuscator.obf.transform.type;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidMethod;

import java.util.List;

public interface TransformerRunner {

    void run(final SkidSession skidSession, final List<SkidMethod> methods);
}
