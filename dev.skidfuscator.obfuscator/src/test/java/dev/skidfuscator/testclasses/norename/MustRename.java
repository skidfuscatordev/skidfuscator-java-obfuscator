package dev.skidfuscator.testclasses.norename;

import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

import java.lang.reflect.Method;

public class MustRename implements TestRun {

    @Exclude
    @Override
    public void run() {
        test();
        verify();
    }

    @Exclude
    public void verify() {
        try {
            for (Method declaredMethod : this.getClass().getDeclaredMethods()) {
                if (declaredMethod.getName().contains("injectParam")) {
                    System.out.println("Found method: " + declaredMethod);
                    assert declaredMethod.getName().equals("injectParam") || declaredMethod
                            .getName().contains("$");
                }
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
        injectParam("Hello, World!", 1);
    }

    @Exclude
    public int injectParam(String param, int value) {
        System.out.println("Injected param: " + param + " value " + value);
        return 1;
    }

    public int injectParam(String param) {
        System.out.println("Injected param: " + param);
        return 1;
    }
}
