package dev.skidfuscator.test.string;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.string.RegularStringTestClass;
import dev.skidfuscator.testclasses.string.StaticStringTestClass;

public class StaticStringTest extends SkidTest {
    @Override
    protected void execute(TestRun run) {
        IllegalStateException failure = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, run::run);
        org.junit.jupiter.api.Assertions.assertEquals("This is a test", failure.getMessage());
    }

    @Override
    public Class<? extends TestRun> getMainClass() {
        return StaticStringTestClass.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{StaticStringTestClass.class};
    }
}
