package dev.skidfuscator.testclasses.opaque;

import dev.skidfuscator.testclasses.TestRun;

import java.util.Arrays;
import java.util.List;

public class OpaqueListClazz implements TestRun {
    private static final List<Integer> list = Arrays.asList(1, 2, 3);

    @Override
    public void run() {
        Integer i = null;
        for (Integer j : list) {
            if (j == 0) {
                i = j;
                break;
            }
        }

        System.out.println(i);
        assert i == null : "i is not null???";
    }
}