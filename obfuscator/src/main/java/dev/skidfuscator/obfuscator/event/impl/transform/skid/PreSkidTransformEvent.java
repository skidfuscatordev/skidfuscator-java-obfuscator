package dev.skidfuscator.obfuscator.event.impl.transform.skid;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.TransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.SkidTransformEvent;

public class PreSkidTransformEvent extends SkidTransformEvent {
    public PreSkidTransformEvent(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }
}
