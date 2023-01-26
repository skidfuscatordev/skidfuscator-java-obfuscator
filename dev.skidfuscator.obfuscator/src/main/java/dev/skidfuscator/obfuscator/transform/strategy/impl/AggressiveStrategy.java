package dev.skidfuscator.obfuscator.transform.strategy.impl;

import dev.skidfuscator.obfuscator.transform.strategy.StrengthStrategy;

public class AggressiveStrategy implements StrengthStrategy {
    @Override
    public boolean shouldExecuteNext() {
        return true;
    }

    @Override
    public boolean execute() {
        return true;
    }

    @Override
    public void reset() {

    }
}
