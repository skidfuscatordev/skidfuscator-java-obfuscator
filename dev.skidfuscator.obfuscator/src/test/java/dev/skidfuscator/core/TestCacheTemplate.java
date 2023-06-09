package dev.skidfuscator.core;

import dev.skidfuscator.annotations.NativeObfuscation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestCacheTemplate {
    private static Map<String, Integer> initialPredicates;

    @NativeObfuscation
    private static void add(final String cache, int predicate) {
        initialPredicates.put(cache, predicate);
    }

    @NativeObfuscation
    public static int get(final String clazz) {
        final int count = initialPredicates.get("count");
        final boolean match = count == TestDispatcher.loaded;

        if (!match) {
            System.out.println("Unmatched: " + count + " vs " + TestDispatcher.loaded);
            return new Random().nextInt();
        }

        initialPredicates.put("count", count + 1);
        return initialPredicates.get(clazz);
    }

    @NativeObfuscation
    private static void init() {
    }

    @NativeObfuscation
    private static void bootstrap() {
        initialPredicates = new HashMap<>();
        initialPredicates.put("count", 1);
    }

    static {
        bootstrap();
        init();
    }
}
