package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ificmpgt;
import dev.skidfuscator.testclasses.conditionals.Ificmple;

public class IficmpleTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ificmple.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ificmple.class
        };
    }
}
