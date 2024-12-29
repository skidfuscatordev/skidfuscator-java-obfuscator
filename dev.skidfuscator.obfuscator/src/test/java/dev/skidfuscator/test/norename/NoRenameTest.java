package dev.skidfuscator.test.norename;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.norename.NoRename;
import dev.skidfuscator.testclasses.opaque.OpaqueListClazz;

public class NoRenameTest extends SkidTest {

    @Override
    public Class<? extends TestRun> getMainClass() {
        return NoRename.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                NoRename.class,
        };
    }
}
