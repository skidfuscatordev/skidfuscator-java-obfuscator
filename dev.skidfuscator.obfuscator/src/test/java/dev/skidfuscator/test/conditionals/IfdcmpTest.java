package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ifacmpne;
import dev.skidfuscator.testclasses.conditionals.Ifdcmp;

public class IfdcmpTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ifdcmp.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ifdcmp.class
        };
    }
}
