package dev.skidfuscator.obfuscator.event.impl.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.TransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;

public abstract class GroupTransformEvent extends TransformEvent {
    private final SkidGroup group;

    public GroupTransformEvent(Skidfuscator skidfuscator, SkidGroup group) {
        super(skidfuscator);
        this.group = group;
    }

    public SkidGroup getGroup() {
        return group;
    }
}
