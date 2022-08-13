package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ificmpne;
import dev.skidfuscator.testclasses.conditionals.Iflcmp;

public class IflcmpTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Iflcmp.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Iflcmp.class
        };
    }
}
