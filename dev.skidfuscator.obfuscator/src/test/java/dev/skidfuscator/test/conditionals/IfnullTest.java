package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ifnonnull;
import dev.skidfuscator.testclasses.conditionals.Ifnull;

public class IfnullTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ifnull.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ifnull.class
        };
    }
}
