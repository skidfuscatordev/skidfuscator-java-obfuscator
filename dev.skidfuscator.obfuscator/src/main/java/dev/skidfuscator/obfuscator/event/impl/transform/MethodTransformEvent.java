package dev.skidfuscator.obfuscator.event.impl.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.TransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;

public abstract class MethodTransformEvent extends TransformEvent {
    private final SkidMethodNode methodNode;

    public MethodTransformEvent(Skidfuscator skidfuscator, SkidMethodNode methodNode) {
        super(skidfuscator);
        this.methodNode = methodNode;
    }

    public SkidMethodNode getMethodNode() {
        return methodNode;
    }
}
