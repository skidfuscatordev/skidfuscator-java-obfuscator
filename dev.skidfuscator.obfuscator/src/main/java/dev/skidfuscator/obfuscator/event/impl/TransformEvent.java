package dev.skidfuscator.obfuscator.event.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class TransformEvent extends Event {
    private int changed;
    private final List<String> issues = new ArrayList<>();

    public TransformEvent(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }

    public void tick() {
        changed++;
    }

    public int getChanged() {
        return changed;
    }

    public void warn(String issue) {
        issues.add(issue);
    }

    public List<String> getIssues() {
        return Collections.unmodifiableList(issues);
    }
}
