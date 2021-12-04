package dev.skidfuscator.test.benchmark;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.Random;

@State(Scope.Thread)
public class XorEncryptionBenchmark {

    private final Random random = new Random();

    @Benchmark
    public int encrypt() {
        final int x = random.nextInt();
        return x ^ 393389373;
    }

    @Benchmark
    public int encrypt2() {
        final int x = random.nextInt();
        return ((x & (7 << 29)) >> 29) | (x << 3);
    }

    @Benchmark
    public int encrypt3() {
        final int x = random.nextInt();
        return (((x * 31) >>> 4) % x) ^ (x >>> 16);
    }
}
