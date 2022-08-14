package dev.skidfuscator.testclasses.exception;

import dev.skidfuscator.testclasses.TestRun;
import org.junit.jupiter.api.Assertions;

public class IntegerFormatExceptionTestClass implements TestRun {
    @Override
    public void run() {
        try {
            Integer.decode(":)");
            Assertions.fail();
        } catch (NumberFormatException e)  {
            return;
        } catch (Exception e) {
            Assertions.fail();
        }
    }
}
