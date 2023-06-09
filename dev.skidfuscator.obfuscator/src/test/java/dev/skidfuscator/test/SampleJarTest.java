package dev.skidfuscator.test;

import com.esotericsoftware.asm.Type;
import dev.skidfuscator.core.TestSkidfuscator;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import dev.skidfuscator.core.classloader.SkidClassLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

public class SampleJarTest {

    @RepeatedTest(5)
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
                .config(new File(TestSkidfuscator.class.getResource("/config/runtime.hocon").getFile()))
                .jmod(MiscUtil.getJavaVersion() > 8)
                .phantom(true)
                .analytics(false)
                .build();

        final Skidfuscator skidfuscator = new Skidfuscator(session);
        skidfuscator.run();

        //assert skidfuscator.getConfig().isDriver() : "Compiled with driver when config says no";

        final URL[] urls = new URL[]{
                output.toURL()
        };

        System.out.println("-----------");

        for (URL url : urls) {
            try (JarInputStream jarInputStream = new JarInputStream(url.openStream())) {
                JarEntry entry;
                while ((entry = jarInputStream.getNextJarEntry()) != null) {
                    System.out.println(entry.getName());
                }
            }
        }

        System.out.println("-----------");

        try(SkidClassLoader classLoader = new SkidClassLoader(urls)) {
            System.out.println(skidfuscator.getClassRemapper());
            final String name = skidfuscator
                    .getClassRemapper()
                    .map(Type.getObjectType("dev/sim0n/evaluator/Main").getInternalName());
            System.out.println("Found named " + name);
            final String replaced = name == null ? "dev.sim0n.evaluator.Main" : name.replace("/", ".");
            final Class<?> clazz = classLoader.loadClass(replaced);
            clazz.getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[0]);
        }
    }
}
