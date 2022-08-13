package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Iflcmp;
import dev.skidfuscator.testclasses.conditionals.Ifnonnull;

public class IfnonnullTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ifnonnull.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ifnonnull.class
        };
    }
}
