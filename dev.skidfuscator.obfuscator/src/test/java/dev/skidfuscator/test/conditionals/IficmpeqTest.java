package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Iffcmp;
import dev.skidfuscator.testclasses.conditionals.Ificmpeq;

public class IficmpeqTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ificmpeq.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ificmpeq.class
        };
    }
}
