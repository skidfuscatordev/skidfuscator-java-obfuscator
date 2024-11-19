package framework.testclasses;

import framework.Impure;
import framework.Pure;

public class StringOperations {
    @Pure(
        description = "String length calculation",
        because = {"String is immutable", "No side effects", "Deterministic result"}
    )
    public static int stringLength(String s) {
        return s.length();
    }

    @Impure(
        description = "Prints to console",
        because = {"System.out is a side effect", "Modifies external state"}
    )
    public static void printString(String s) {
        System.out.println(s);
    }
}