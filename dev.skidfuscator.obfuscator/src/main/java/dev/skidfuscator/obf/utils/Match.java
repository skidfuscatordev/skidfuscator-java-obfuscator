package dev.skidfuscator.obf.utils;

import java.util.ArrayList;
import java.util.List;

public class Match {
    private final String tested;
    private final List<String> matches;

    public static Match of(String var) {
        return new Match(var);
    }

    public static boolean fast(String var, String... vars) {
        final Match match = new Match(var);
        for (String s : vars) {
            match.match(s);
        }

        return match.check();
    }

    private Match(String tested) {
        this.tested = tested;
        this.matches = new ArrayList<>();
    }

    public Match match(String var) {
        this.matches.add(var);
        return this;
    }

    public Match match(String var, boolean predicate) {
        if (predicate)
            this.matches.add(var);
        return this;
    }

    public boolean check() {
        for (String match : matches) {
            if (!tested.contains(match)) return false;
        }

        return true;
    }
}
