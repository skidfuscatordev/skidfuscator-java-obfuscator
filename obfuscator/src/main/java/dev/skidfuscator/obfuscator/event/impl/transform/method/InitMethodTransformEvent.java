package dev.skidfuscator.obfuscator.event.impl.transform.method;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.transform.MethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;

public class InitMethodTransformEvent extends MethodTransformEvent {
    public InitMethodTransformEvent(Skidfuscator skidfuscator, SkidMethodNode methodNode) {
        super(skidfuscator, methodNode);
    }
}
