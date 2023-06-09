package dev.skidfuscator.obfuscator.predicate.cache;

import java.util.HashMap;
import java.util.Map;

public class CacheTemplate {
    private static Map<String, Integer> initialPredicates;

    private static void add(final String cache, int predicate) {
        initialPredicates.put(cache, predicate);
    }

    public static int get(final String clazz) {
        final int count = initialPredicates.get("count");
        initialPredicates.put("count", count + 1);
        return initialPredicates.get(clazz);
    }

    private static void init() {
    }

    private static void bootstrap() {
        initialPredicates = new HashMap<>();
        initialPredicates.put("count", 1);
    }

    static {
        bootstrap();
        init();
    }
}
