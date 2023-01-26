package dev.skidfuscator.testclasses.evaluator.test.impl.string;

import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.evaluator.test.TestHandler;

import java.util.Arrays;
import java.util.Base64;

public class StringTest implements TestHandler {
    private static final String realString = "Œüèé€ìàò";

    @Override
    public void handle() {
        final String decoded = getRealString();
        final String toObfuscate = "Œüèé€ìàò";

        assert decoded.equals(toObfuscate) : "Failed decryption (Had: " + toObfuscate + " expected: " + getRealString();

        System.out.println("Passed string encryption test with " + toObfuscate);
    }

    @Exclude
    private String getRealString() {
        return realString;
    }
}
