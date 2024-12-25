package dev.skidfuscator.testclasses.loop;

import dev.skidfuscator.testclasses.TestRun;

public class LoopConditionTestClass implements TestRun {
    @Override
    public void run() {
        // Test simple for loop
        int sum1 = 0;
        for (int i = 0; i < 5; i++) {
            sum1 += i;
        }
        assert sum1 == 10 : "Failed simple for loop test";

        // Test while loop with variable condition
        int sum2 = 0;
        int count = 0;
        int max = 5;
        while (count < max) {
            sum2 += count;
            count++;
        }
        assert sum2 == 10 : "Failed while loop test";

        // Test do-while loop
        int sum3 = 0;
        int j = 0;
        do {
            sum3 += j;
            j++;
        } while (j < 5);
        assert sum3 == 10 : "Failed do-while loop test";

        // Test nested loops
        int sum4 = 0;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 2; y++) {
                sum4 += x + y;
            }
        }
        assert sum4 == 9 : "Failed nested loops test";

        // Test loop with method call in condition
        int sum5 = 0;
        int k = 0;
        while (k < getLimit()) {
            sum5 += k;
            k++;
        }
        assert sum5 == 10 : "Failed loop with method call test";
    }

    private int getLimit() {
        return 5;
    }
} 