package framework.testclasses;

import framework.Impure;
import framework.Pure;

public class MathOperations {
    @Pure(
        description = "Simple addition of two numbers",
        because = {"Uses only primitive parameters", "No side effects", "Returns primitive result"}
    )
    public static int add(int a, int b) {
        return a + b;
    }

    @Pure(
        description = "Recursive factorial calculation",
        because = {"Uses only primitive parameters", "Recursion is pure", "No side effects"}
    )
    public static int factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }


    private static int counter = 0;

    @Impure(
            description = "Modifies static counter",
            because = {"Modifies static state", "Return value depends on state"}
    )
    public static int increment() {
        return counter++;
    }
}