package dev.skidfuscator.obfuscator.transform.strategy.impl;

import dev.skidfuscator.obfuscator.transform.strategy.StrengthStrategy;
import dev.skidfuscator.obfuscator.util.RandomUtil;

public class WeakRandomSkipStrategy implements StrengthStrategy {
    private boolean wasLast;
    private Boolean isNext;

    @Override
    public boolean shouldExecuteNext() {
        if (isNext == null) {
            if (wasLast) {
                isNext = RandomUtil.nextBoolean();
            } else {
                isNext = true;
            }
        }

        return isNext;
    }

    @Override
    public boolean execute() {
        final boolean result = shouldExecuteNext();
        wasLast = result;
        isNext = null;

        return result;
    }

    @Override
    public void reset() {
        wasLast = false;
        isNext = null;
    }
}
