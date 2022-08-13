package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.LookupswitchNoDefault;
import dev.skidfuscator.testclasses.conditionals.Tableswitch;

public class TableswitchTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Tableswitch.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Tableswitch.class
        };
    }
}
