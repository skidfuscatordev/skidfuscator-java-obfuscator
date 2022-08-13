package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ificmple;
import dev.skidfuscator.testclasses.conditionals.Ificmplt;

public class IficmpltTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ificmplt.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ificmplt.class
        };
    }
}
