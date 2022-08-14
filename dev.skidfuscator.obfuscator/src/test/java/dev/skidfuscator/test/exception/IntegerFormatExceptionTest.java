package dev.skidfuscator.test.exception;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.exception.IntegerFormatExceptionTestClass;

public class IntegerFormatExceptionTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return IntegerFormatExceptionTestClass.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                IntegerFormatExceptionTestClass.class
        };
    }
}
