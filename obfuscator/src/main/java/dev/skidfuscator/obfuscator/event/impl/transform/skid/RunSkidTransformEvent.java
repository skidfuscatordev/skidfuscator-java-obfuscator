package dev.skidfuscator.obfuscator.event.impl.transform.skid;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.transform.SkidTransformEvent;

public class RunSkidTransformEvent extends SkidTransformEvent {
    public RunSkidTransformEvent(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }
}
