package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import dev.skidfuscator.test.classloader.SkidClassLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

public class SampleJarTest {

    @Test
    @Disabled
    public void test2() throws Exception {
        final File input = new File("src/test/resources/test.jar");
        final File output = new File("src/test/resources/test-out.jar");

        final File runtime = new File(
                new File(System.getProperty("java.home")),
                MiscUtil.getJavaVersion() > 8
                        ? "jmods"
                        : "lib/rt.jar"
        );

        final SkidfuscatorSession session = SkidfuscatorSession
                .builder()
                .input(input)
                .output(output)
                .runtime(runtime)
                .jmod(MiscUtil.getJavaVersion() > 8)
                .phantom(true)
                .analytics(false)
                .build();

        final Skidfuscator skidfuscator = new Skidfuscator(session);
        skidfuscator.run();

        final URL[] urls = new URL[]{
                output.toURL()
        };

        try(SkidClassLoader classLoader = new SkidClassLoader(urls)) {
            final Class<?> clazz = classLoader.loadClass("dev.skidfuscator.testclasses.evaluator.EvaluatorMain");
            clazz.getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[0]);
        }
    }
}
