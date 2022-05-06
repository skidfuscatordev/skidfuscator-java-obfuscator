package dev.skidfuscator.obfuscator.event.impl.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.TransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;

public abstract class SkidTransformEvent extends TransformEvent {
    public SkidTransformEvent(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }
}
