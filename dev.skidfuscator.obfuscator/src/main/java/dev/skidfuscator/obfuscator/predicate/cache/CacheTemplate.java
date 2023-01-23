package dev.skidfuscator.obfuscator.predicate.cache;

import dev.skidfuscator.annotations.NativeObfuscation;

import java.util.HashMap;
import java.util.Map;

public class CacheTemplate {
    private static Map<String, Integer> initialPredicates;

    @NativeObfuscation
    private static void add(final String cache, int predicate) {
        initialPredicates.put(cache, predicate);
    }

    @NativeObfuscation
    public static int get(final String clazz) {
        return initialPredicates.get(clazz);
    }

    @NativeObfuscation
    private static void init() {
    }

    @NativeObfuscation
    private static void bootstrap() {
        initialPredicates = new HashMap<>();
    }

    static {
        bootstrap();
        init();
    }
}
