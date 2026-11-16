package dev.skidfuscator.testclasses.jar;

/** Source-controlled input for the executable JAR regression test. */
public class SampleApp {
    public static void main(String[] args) {
        int sum = 0;
        for (int i = 0; i <= 10; i++) sum += i;
        System.out.println("protected:" + sum);
    }
}
