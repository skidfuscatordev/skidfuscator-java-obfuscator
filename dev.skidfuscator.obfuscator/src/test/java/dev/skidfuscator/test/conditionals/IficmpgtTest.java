package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ificmpge;
import dev.skidfuscator.testclasses.conditionals.Ificmpgt;

public class IficmpgtTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ificmpgt.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ificmpgt.class
        };
    }
}
