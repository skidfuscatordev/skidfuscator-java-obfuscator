package dev.skidfuscator.testclasses.norename;

import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.evaluator.test.TestHandler;

import java.lang.reflect.Method;

public class NoRename implements TestRun {

    @Exclude
    @Override
    public void run() {
        test();
        verify();
    }

    @Exclude
    public void verify() {
        try {
            final Method method = this.getClass().getDeclaredMethod("injectParam", String.class, int.class);

            if (method == null) {
                throw new IllegalStateException("Failed to verify number!");
            }
        } catch (Exception e) {
            try {
                final Method method = this.getClass().getDeclaredMethod("injectParam", String.class);
                System.out.println("Default Method found: " + method);
                return;
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to verify!", ex);
            }
        }
    }

    public void test() {
        injectParam("Hello, World!");
    }

    public int injectParam(String param) {
        System.out.println("Injected param: " + param);
        return 1;
    }
}
