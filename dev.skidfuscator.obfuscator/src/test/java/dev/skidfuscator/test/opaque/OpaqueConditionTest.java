package dev.skidfuscator.test.opaque;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.opaque.OpaqueConditionClazz;

public class OpaqueConditionTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return OpaqueConditionClazz.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{OpaqueConditionClazz.class};
    }
}
