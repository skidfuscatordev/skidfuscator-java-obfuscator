package dev.skidfuscator.obfuscator.util.misc;

public class Counter {
    private int count;

    public Counter(int count) {
        this.count = count;
    }

    public Counter() {
        this.count = 0;
    }

    public void tick() {
        this.count++;
    }

    public void reset() {
        this.count = 0;
    }

    public int get() {
        return count;
    }
}
