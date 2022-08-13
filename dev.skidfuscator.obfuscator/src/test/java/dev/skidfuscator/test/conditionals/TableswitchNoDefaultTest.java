package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Tableswitch;
import dev.skidfuscator.testclasses.conditionals.TableswitchNoDefault;

public class TableswitchNoDefaultTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return TableswitchNoDefault.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                TableswitchNoDefault.class
        };
    }
}
