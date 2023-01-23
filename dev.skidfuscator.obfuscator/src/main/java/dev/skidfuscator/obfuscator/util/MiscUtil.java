package dev.skidfuscator.obfuscator.util;

import com.google.common.base.CaseFormat;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Locale;

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
    public String fixedLengthString(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }

    public String appendColor(final String string, final String color) {
        return color + string + "\033[0m" /* reset color ansi*/;
    }

    public String replaceColor(final String string, final String replace, final String color) {
        return string.replace(replace, color + replace + "\033[0m" /* reset color ansi*/);
    }

    public int getJavaVersion() {
        String version = System.getProperty("java.version");
        return decodeJvmVersion(version);
    }

    public int decodeJvmVersion(String version) {
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }

    public boolean isJmod() {
        return MiscUtil.getJavaVersion() > 8;
    }

    public String toCamelCase(final String s) {
        return CaseFormat.UPPER_UNDERSCORE.to(
                CaseFormat.LOWER_CAMEL,
                s.toUpperCase(Locale.ROOT).replace(' ', '_')
        );
    }
}
