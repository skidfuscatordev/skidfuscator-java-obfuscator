package dev.skidfuscator.test.java_lang_String;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.conditionals.Goto;
import dev.skidfuscator.testclasses.java_lang_String.EqualsTestClass;

public class EqualsTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return EqualsTestClass.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                EqualsTestClass.class,
                RandomUtil.class
        };
    }
}
