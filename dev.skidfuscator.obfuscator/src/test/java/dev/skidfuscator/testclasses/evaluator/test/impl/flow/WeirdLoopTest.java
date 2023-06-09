package dev.skidfuscator.testclasses.evaluator.test.impl.flow;

import dev.skidfuscator.testclasses.evaluator.test.TestHandler;

import java.util.Random;

public class WeirdLoopTest implements TestHandler {
    private int index;
    @Override
    public void handle() {
        int[] targets = new int[new Random().nextInt(5) + 2];

        assert targets.length < 100 : "Targets must be less than 100 in size!";
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new Random().nextInt();
        }

        int old = this.index;
        int depth = 0;
        while (targets[this.index] == targets[old]) {
            this.index = new Random().nextInt(targets.length);

            if (depth > 100) {
                throw new IllegalStateException("Depth overflow");
            }

            depth++;
        }

        assert old != this.index : "Index cannot be duplicated";
    }
}
