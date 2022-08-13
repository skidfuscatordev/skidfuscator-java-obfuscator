package dev.skidfuscator.testclasses.evaluator.test.impl.string;

import dev.skidfuscator.testclasses.evaluator.test.Test;

import java.util.Base64;

public class StringTest implements Test {
    private static final String realString = "xZLDvMOow6nigqzDrMOgw7I=";

    @Override
    public void handle() {
        final String decoded = new String(Base64.getDecoder().decode(realString));
        final String toObfuscate = "Œüèé€ìàò";

        assert decoded.equals(toObfuscate);

        System.out.println("Passed string encryption test with " + toObfuscate);
    }
}
