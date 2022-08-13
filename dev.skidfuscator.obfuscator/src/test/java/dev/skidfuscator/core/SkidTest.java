package dev.skidfuscator.core;

import dev.skidfuscator.test.classloader.SkidClassLoader;
import dev.skidfuscator.testclasses.TestRun;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Map;

public abstract class SkidTest implements TestCase {
    @Test
    public void test() {
        final TestSkidfuscator skidfuscator = new TestSkidfuscator(this);
        skidfuscator.run();
    }

    @Override
    public void receiveAndExecute(Map<String, byte[]> output) {
        final URL[] urls = new URL[0];

        try(SkidClassLoader classLoader = new SkidClassLoader(urls)) {
            for (Map.Entry<String, byte[]> entry : output.entrySet()) {
                final String name = entry.getKey();
                final byte[] clazzData = entry.getValue();

                classLoader.defineClass(name, clazzData);
            }

            final Class<?> clazz = classLoader.loadClass(this.getMainClass().getName());
            final TestRun run = (TestRun) clazz.newInstance();
            run.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed execution", e);
        }
    }
}
