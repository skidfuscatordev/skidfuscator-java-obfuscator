package dev.skidfuscator.test;

import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.runner.options.WarmupMode;

public class EncryptionBenchmarkTest {

    //@Test
    public void runTest() throws RunnerException {
        final Options options = new OptionsBuilder()
                .mode(Mode.SingleShotTime)
                .verbosity(VerboseMode.NORMAL)
                .measurementIterations(10000)
                .measurementBatchSize(1000)
                .operationsPerInvocation(1)
                .resultFormat(ResultFormatType.TEXT)
                .build();

        final Runner runner = new Runner(options);
        runner.run();
    }
}
