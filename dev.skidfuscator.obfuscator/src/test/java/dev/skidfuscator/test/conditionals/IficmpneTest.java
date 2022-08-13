package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ificmplt;
import dev.skidfuscator.testclasses.conditionals.Ificmpne;

public class IficmpneTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ificmpne.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ificmpne.class
        };
    }
}
