package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Goto;

public class GotoTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Goto.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Goto.class
        };
    }
}
