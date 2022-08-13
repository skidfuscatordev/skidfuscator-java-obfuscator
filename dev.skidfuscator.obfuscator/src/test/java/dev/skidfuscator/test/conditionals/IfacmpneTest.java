package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ifacmpne;

public class IfacmpneTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Ifacmpne.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Ifacmpne.class
        };
    }
}
