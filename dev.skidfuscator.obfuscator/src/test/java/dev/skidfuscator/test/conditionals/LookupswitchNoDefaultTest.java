package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Lookupswitch;
import dev.skidfuscator.testclasses.conditionals.LookupswitchNoDefault;

public class LookupswitchNoDefaultTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return LookupswitchNoDefault.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                LookupswitchNoDefault.class
        };
    }
}
