package dev.skidfuscator.obfuscator.transform.strategy.impl;

import dev.skidfuscator.obfuscator.transform.strategy.StrengthStrategy;
import dev.skidfuscator.obfuscator.util.RandomUtil;

public class NormalStrategy implements StrengthStrategy {
    private boolean executed = false;
    @Override
    public boolean shouldExecuteNext() {
        return !executed;
    }

    @Override
    public boolean execute() {
        final boolean talk = executed;
        executed = false;
        return talk;
    }

    @Override
    public void reset() {
        executed = false;
    }
}
