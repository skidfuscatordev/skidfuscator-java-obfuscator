package dev.skidfuscator.obfuscator.util.match;

import com.google.gson.internal.Streams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Match {
    private final String tested;
    private final Map<String, Boolean> matches;

    public static Match of(String var) {
        return new Match(var);
    }

    public static boolean fast(String var, String... vars) {
        final Match match = new Match(var);
        for (String s : vars) {
            match.match(s, true);
        }

        return match.check();
    }

    private Match(String tested) {
        this.tested = tested;
        this.matches = new HashMap<>();
    }

    public Match match(String var) {
        this.matches.put(var, true);
        return this;
    }

    public Match match(String var, boolean predicate) {
        this.matches.put(var, predicate);
        return this;
    }

    public boolean check() {
        for (Map.Entry<String, Boolean> entry : matches.entrySet()) {
            final String match = entry.getKey();
            final boolean predicate = entry.getValue();

            if (predicate)
                continue;

            if (tested.contains(match))
                return false;
        }

        return true;
    }
}