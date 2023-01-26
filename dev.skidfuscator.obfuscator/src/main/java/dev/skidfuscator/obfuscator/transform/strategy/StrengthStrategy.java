package dev.skidfuscator.obfuscator.transform.strategy;

public interface StrengthStrategy {
    boolean shouldExecuteNext();

    boolean execute();

    void reset();
}
