package dev.skidfuscator.obfuscator.util.progress;

import java.io.Closeable;

public interface ProgressWrapper extends AutoCloseable {
    void tick();

    void tick(final int amount);

    @Override
    default void close() {

    }
}
