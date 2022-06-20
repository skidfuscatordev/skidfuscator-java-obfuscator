package dev.skidfuscator.obfuscator.event.impl.transform.skid;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.impl.transform.SkidTransformEvent;

public class PostSkidTransformEvent extends SkidTransformEvent {
    public PostSkidTransformEvent(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }
}
