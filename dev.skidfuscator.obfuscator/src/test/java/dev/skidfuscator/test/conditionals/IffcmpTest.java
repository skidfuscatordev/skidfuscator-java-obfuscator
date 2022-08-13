package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ifdcmp;
import dev.skidfuscator.testclasses.conditionals.Iffcmp;

public class IffcmpTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Iffcmp.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Iffcmp.class
        };
    }
}
