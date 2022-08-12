package dev.skidfuscator.jghost;

public interface GhostMerger<T> {
    T merge(T implementation, T other);
}
