package dev.skidfuscator.obfuscator.order;

import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;

public interface OrderAnalysis {
    MethodType getType(final SkidMethodNode methodNode);

}
