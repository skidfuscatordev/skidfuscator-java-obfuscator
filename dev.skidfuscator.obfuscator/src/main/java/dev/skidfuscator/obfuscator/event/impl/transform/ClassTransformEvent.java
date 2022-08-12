package dev.skidfuscator.obfuscator.event.impl.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.TransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;

public abstract class ClassTransformEvent extends TransformEvent {
    private final SkidClassNode classNode;

    public ClassTransformEvent(Skidfuscator skidfuscator, SkidClassNode classNode) {
        super(skidfuscator);
        this.classNode = classNode;
    }

    public SkidClassNode getClassNode() {
        return classNode;
    }
}
