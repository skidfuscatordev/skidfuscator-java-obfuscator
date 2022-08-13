package dev.skidfuscator.testclasses.conditionals;

import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.testclasses.TestRun;

public class Goto implements TestRun {
    @Override
    public void run() {
        assert exec(10) == 10 : "Failed to match initial ret vat";

        for (int i = 0; i < 64; i++) {
            final int random = RandomUtil.nextInt();

            if (random == 10)
                continue;

            assert exec(random) == 0 : "Failed to match ret break";
        }
    }

    public int exec(int value) {
        int retval = 0;
        int a = 10;
        int decrement = 1;
        while (a > 0) {
            if (a == 1 && value == 10)
                retval = 10;
            a -= decrement;
        }
        return retval;
    }

}
