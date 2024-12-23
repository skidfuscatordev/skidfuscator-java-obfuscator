package dev.skidfuscator.obfuscator.transform.impl.pure;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventPriority;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.InitSkidTransformEvent;
import dev.skidfuscator.obfuscator.number.pure.VmHashTransformer;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;

public class PureHashTransformer extends AbstractTransformer {
    public PureHashTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Pure Encryption");
    }

    @Listen
    void handle(final InitSkidTransformEvent event) {
        if (skidfuscator.getVmHasher() == null) {
            throw new IllegalStateException("VmHasher is null");
        }
        skidfuscator.setVmHasher(new VmHashTransformer(skidfuscator));
    }
}
