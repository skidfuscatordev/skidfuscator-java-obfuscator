package dev.skidfuscator.test.loop;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.java_lang_String.EqualsTestClass;
import dev.skidfuscator.testclasses.loop.LoopConditionTestClass;

public class LoopTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return LoopConditionTestClass.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                LoopConditionTestClass.class,
                RandomUtil.class
        };
    }
}
