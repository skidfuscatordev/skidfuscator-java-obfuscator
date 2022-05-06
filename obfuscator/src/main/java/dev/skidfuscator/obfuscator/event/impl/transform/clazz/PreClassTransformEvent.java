package dev.skidfuscator.obfuscator.event.impl.transform.clazz;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.transform.ClassTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;

public class PreClassTransformEvent extends ClassTransformEvent {
    public PreClassTransformEvent(Skidfuscator skidfuscator, SkidClassNode classNode) {
        super(skidfuscator, classNode);
    }
}
