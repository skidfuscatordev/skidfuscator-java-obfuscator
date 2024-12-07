package dev.skidfuscator.test.norename;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.norename.NoPredicate;
import dev.skidfuscator.testclasses.norename.NoRename;

public class NoPredicateTest extends SkidTest {

    @Override
    public Class<? extends TestRun> getMainClass() {
        return NoPredicate.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                NoPredicate.class,
        };
    }

    @Override
    public String getConfigPath() {
        return "/config/runtime_no_predicate.hocon";
    }
}
