package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ificmpeq;
import dev.skidfuscator.testclasses.conditionals.Ificmpge;

public class IficmpgeTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ificmpge.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ificmpge.class
        };
    }
}
