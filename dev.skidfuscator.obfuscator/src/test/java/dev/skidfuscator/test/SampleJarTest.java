package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import dev.skidfuscator.test.classloader.SkidClassLoader;
import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.asm.Modifier;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.fs.HostFileDescriptorManager;
import dev.xdark.ssvm.jit.JitClass;
import dev.xdark.ssvm.jit.JitCompiler;
import dev.xdark.ssvm.jit.JitInstaller;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.mirror.JavaMethod;
import dev.xdark.ssvm.symbol.VMSymbols;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.ObjectValue;
import dev.xdark.ssvm.value.Value;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

public class SampleJarTest {

    @Test
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
            final Class<?> clazz = classLoader.loadClass("dev.sim0n.evaluator.Main");
            clazz.getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[0]);
        }
    }
}
