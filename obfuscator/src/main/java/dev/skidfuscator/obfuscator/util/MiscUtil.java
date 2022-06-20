package dev.skidfuscator.obfuscator.util;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.util.List;

@UtilityClass
public class MiscUtil {

    public <T> T[] toArray(Class<T> type, List<T> collection) {
        T[] array = (T[]) Array.newInstance(type, collection.size());
        for (int i = 0; i < collection.size(); i++) {
            array[i] = collection.get(i);
        }

        return array;
    }

    public <T> int indexOf(T[] arr, T t) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == t) return i;
        }

        return -1;
    }

    public int getJavaVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }
}
