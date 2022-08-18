package dev.skidfuscator.obfuscator.util.progress;

import java.io.Closeable;

public interface ProgressWrapper extends AutoCloseable {
    void tick();

    void tick(final int amount);

    default void fail() {
        fail(null);
    }

    default void fail(final Throwable exception) {}

    default void succeed() {}

    @Override
    default void close() {

    }
}
