package dev.skidfuscator.testclasses.java_lang_String;

import dev.skidfuscator.testclasses.TestRun;

public class EnumNameTestClass implements TestRun {
    private enum TestEnum {
        FIRST_VALUE,
        SECOND_VALUE,
        THIRD_VALUE
    }

    @Override
    public void run() {
        // Test direct enum name comparison
        assert TestEnum.FIRST_VALUE.name().equals("FIRST_VALUE") : "Failed enum name test #1";
        
        // Test enum name comparison with variable
        TestEnum value = TestEnum.SECOND_VALUE;
        assert value.name().equals("SECOND_VALUE") : "Failed enum name test #2";
        
        // Test enum name comparison with method call
        assert getEnumValue().name().equals("THIRD_VALUE") : "Failed enum name test #3";
    }

    private TestEnum getEnumValue() {
        return TestEnum.THIRD_VALUE;
    }
} 