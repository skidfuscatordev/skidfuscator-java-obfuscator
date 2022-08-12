package dev.skidfuscator.obfuscator.event.impl.transform.method;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.transform.MethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;

public class PostMethodTransformEvent extends MethodTransformEvent {
    public PostMethodTransformEvent(Skidfuscator skidfuscator, SkidMethodNode methodNode) {
        super(skidfuscator, methodNode);
    }
}
