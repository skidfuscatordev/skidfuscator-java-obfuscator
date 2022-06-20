package dev.skidfuscator.obfuscator.util.match;

import java.util.HashMap;
import java.util.Map;

public class MatchPair {
    private final String tested;
    private final Map<String, Boolean> matches;

    public static MatchPair of(String var) {
        return new MatchPair(var);
    }

    public static boolean fast(String var, String... vars) {
        final MatchPair match = new MatchPair(var);
        for (String s : vars) {
            match.match(s, true);
        }

        return match.check();
    }

    private MatchPair(String tested) {
        this.tested = tested;
        this.matches = new HashMap<>();
    }

    public MatchPair match(String var, boolean value) {
        this.matches.put(var, value);
        return this;
    }

    public boolean check() {
        for (Map.Entry<String, Boolean> stringBooleanEntry : matches.entrySet()) {
            if (tested.contains(stringBooleanEntry.getKey()) != stringBooleanEntry.getValue())
                return false;
        }
        return true;
    }
}