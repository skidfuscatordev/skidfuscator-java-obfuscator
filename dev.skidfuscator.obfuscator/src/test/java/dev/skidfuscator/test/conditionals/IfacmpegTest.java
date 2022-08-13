package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Goto;
import dev.skidfuscator.testclasses.conditionals.Ifacmpeq;

public class IfacmpegTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ifacmpeq.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ifacmpeq.class
        };
    }
}
