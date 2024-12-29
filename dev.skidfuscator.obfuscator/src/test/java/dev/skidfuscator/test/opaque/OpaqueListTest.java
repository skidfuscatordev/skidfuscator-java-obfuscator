package dev.skidfuscator.test.opaque;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.opaque.OpaqueListClazz;

public class OpaqueListTest extends SkidTest {

    @Override
    public Class<? extends TestRun> getMainClass() {
        return OpaqueListClazz.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                OpaqueListClazz.class,
        };
    }
}
