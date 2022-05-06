package dev.skidfuscator.obfuscator.event.impl.transform.group;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.transform.GroupTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;

public class RunGroupTransformEvent extends GroupTransformEvent {
    public RunGroupTransformEvent(Skidfuscator skidfuscator, SkidGroup group) {
        super(skidfuscator, group);
    }
}
