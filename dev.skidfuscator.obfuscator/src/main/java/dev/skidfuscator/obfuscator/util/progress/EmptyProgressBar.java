package dev.skidfuscator.obfuscator.util.progress;

import me.tongfei.progressbar.ProgressBar;


public class EmptyProgressBar implements ProgressWrapper, AutoCloseable {
    public EmptyProgressBar() {
    }

    @Override
    public void tick() {

    }

    @Override
    public void tick(int amount) {

    }

    @Override
    public void close() {

    }
}
