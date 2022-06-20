package dev.skidfuscator.obfuscator.event.impl.transform.skid;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.transform.SkidTransformEvent;

public class InitSkidTransformEvent extends SkidTransformEvent {
    public InitSkidTransformEvent(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }
}
