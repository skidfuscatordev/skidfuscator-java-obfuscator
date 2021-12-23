package dev.skidfuscator.obf.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MiscUtils {

    public <T> int indexOf(T[] arr, T t) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == t) return i;
        }

        return -1;
    }
}
