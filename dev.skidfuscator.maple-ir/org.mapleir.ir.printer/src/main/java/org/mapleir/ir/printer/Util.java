package org.mapleir.ir.printer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.Opcodes;

public class Util {

    private static final Map<String, Integer> ACCESS_FLAGS;

    static {
        Map<String, Integer> _accessFlags = new HashMap<>();
        try {
            for (Field f : Opcodes.class.getDeclaredFields()) {
                if (f.getName().startsWith("ACC_")) {
                    f.setAccessible(true);

                    String key = f.getName();
                    int val = f.getInt(null);

                    if (!_accessFlags.containsKey(key)) {
                        _accessFlags.put(key, val);
                    } else {
                        throw new IllegalStateException(
                                String.format("Duplicate accessflag, prev=0x%s, val=0x%s, key=%s",
                                        Integer.toHexString(_accessFlags.get(key)),
                                        Integer.toHexString(val), key));
                    }
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException("Error reading access flag fields", e);
        }
        Map<String, Integer> accessFlags = new TreeMap<>(
                (k1, k2) -> Integer.compare(_accessFlags.get(k1), _accessFlags.get(k2)));
        accessFlags.putAll(_accessFlags);

        ACCESS_FLAGS = Collections.unmodifiableMap(accessFlags);
    }

    public static boolean isNonEmpty(Collection<?> col) {
        return col != null && col.size() > 0;
    }

    public static <T> boolean isNonEmpty(T[] arr) {
        return arr != null && arr.length > 0;
    }

    public static String[] asOpcodesAccessFieldFormat(String[] accessFieldNames) {
        String[] res = new String[accessFieldNames.length];
        for (int i = 0; i < accessFieldNames.length; i++) {
            res[i] = "ACC_" + (accessFieldNames[i].toUpperCase());
        }
        return res;
    }

    public static Map<Integer, String> decodeASMFlags(String[] opcodesFieldNames) {
        /* need to do it by name instead of value because
         * there is overlap in the field values. */
        Map<Integer, String> result = new LinkedHashMap<>();
        for (String n : opcodesFieldNames) {
            if (ACCESS_FLAGS.containsKey(n)) {
                result.put(ACCESS_FLAGS.get(n), n);
            } else {
                throw new UnsupportedOperationException(
                        String.format("No flag value for key: %s", n));
            }
        }
        return result;
    }
}
