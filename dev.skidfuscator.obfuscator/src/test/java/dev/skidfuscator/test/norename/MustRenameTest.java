package dev.skidfuscator.test.norename;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.norename.MustRename;
import dev.skidfuscator.testclasses.norename.NoRename;

public class MustRenameTest extends SkidTest {

    @Override
    public Class<? extends TestRun> getMainClass() {
        return MustRename.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                MustRename.class,
        };
    }
}
