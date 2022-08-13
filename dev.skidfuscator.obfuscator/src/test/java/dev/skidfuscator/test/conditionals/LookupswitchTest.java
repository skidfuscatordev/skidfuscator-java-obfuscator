package dev.skidfuscator.test.conditionals;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Ifnull;
import dev.skidfuscator.testclasses.conditionals.Lookupswitch;

public class LookupswitchTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return Lookupswitch.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                Lookupswitch.class
        };
    }
}
