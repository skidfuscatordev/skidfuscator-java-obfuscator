package dev.skidfuscator.testclasses.norename;

import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

import java.lang.reflect.Method;

public class NoPredicate implements TestRun {

    @Exclude
    @Override
    public void run() {
        test();
        verify();
    }

    @Exclude
    public void verify() {
        try {
            final Method method = this.getClass().getDeclaredMethod("injectParam", String.class);

            if (method == null) {
                throw new IllegalStateException("Failed to verify number!");
            }
        } catch (Exception e) {
            try {
                final Method method = this.getClass().getDeclaredMethod("injectParam", String.class, int.class);
                throw new IllegalStateException("Failed to verify! Config failed...");
            } catch (NoSuchMethodException ex) {
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
