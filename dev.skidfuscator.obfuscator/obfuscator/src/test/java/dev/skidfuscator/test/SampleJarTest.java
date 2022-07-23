package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
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
import org.junit.Test;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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
        final SkidfuscatorSession session = SkidfuscatorSession
                .builder()
                .input(input)
                .output(output)
                .runtime(new File(System.getProperty("java.home"), "lib/rt.jar"))
                .phantom(true)
                .analytics(false)
                .build();

        final Skidfuscator skidfuscator = new Skidfuscator(session);
        skidfuscator.run();

        // TODO: Fix SSVM
        if (true)
            return;

        VirtualMachine vm = new VirtualMachine();
        VMHelper helper = vm.getHelper();
        try {
            VMInterface vmi = vm.getInterface();
            // Enable JIT, if needed
            JitClassLoader definer = new JitClassLoader();
            vmi.registerMethodEnter(
                    ctx -> {
                        JavaMethod jm = ctx.getMethod();
                        int count = jm.getInvocationCount();
                        if (count == 256 && !Modifier.isCompiledMethod(jm.getAccess())) {
                            if (JitCompiler.isCompilable(jm)) {
                                try {
                                    JitClass jit = JitCompiler.compile(jm, 3);
                                    JitInstaller.install(jm, definer, jit);
                                } catch (MethodTooLargeException ex) {
                                    MethodNode node = jm.getNode();
                                    node.access |= Modifier.ACC_JIT;
                                } catch (Throwable ex) {
                                    throw new IllegalStateException("Could not install JIT class for " + jm, ex);
                                }
                            }
                        }
                    });
            // Bootstrap VM
            vm.bootstrap();
            VMSymbols symbols = vm.getSymbols();

            // Add jar to system class loader
            Value cl = helper
                            .invokeStatic(
                                    symbols.java_lang_ClassLoader(),
                                    "getSystemClassLoader",
                                    "()Ljava/lang/ClassLoader;",
                                    new Value[0],
                                    new Value[0])
                            .getResult();
            assert cl instanceof ObjectValue : "ClassLoader must be ObjectValue";
            addURL(vm, cl, output.getPath());

            // Invoke main, setup hooks to do stuff, etc
            InstanceJavaClass klass = (InstanceJavaClass) helper.findClass((ObjectValue) cl, "dev.sim0n.evaluator.Main", true);
            JavaMethod method = klass.getStaticMethod("main", "([Ljava/lang/String;)V");

            helper.invokeStatic(
                    klass, method, new Value[0], new Value[] {helper.emptyArray(symbols.java_lang_String())});
        } catch (VMException ex) {
            helper.invokeVirtual("printStackTrace", "()V", new Value[0], new Value[] {ex.getOop()});
            throw ex;
        }
    }

    private static final class JitClassLoader extends ClassLoader
            implements JitInstaller.ClassDefiner {

        @Override
        public Class<?> define(JitClass jitClass) {
            byte[] code = jitClass.getCode();
            return defineClass(jitClass.getClassName().replace('/', '.'), code, 0, code.length);
        }
    }

    private static void addURL(VirtualMachine vm, Value loader, String path) {
        // ((URLClassLoader)loader).addURL(new File(path).toURI().toURL());
        VMHelper helper = vm.getHelper();
        InstanceJavaClass fileClass = (InstanceJavaClass) vm.findBootstrapClass("java/io/File", true);
        InstanceValue file = vm.getMemoryManager().newInstance(fileClass);
        helper.invokeExact(
                fileClass,
                "<init>",
                "(Ljava/lang/String;)V",
                new Value[0],
                new Value[] {file, helper.newUtf8(path)});
        Value uri =
                helper
                        .invokeVirtual("toURI", "()Ljava/net/URI;", new Value[0], new Value[] {file})
                        .getResult();
        Value url =
                helper
                        .invokeVirtual("toURL", "()Ljava/net/URL;", new Value[0], new Value[] {uri})
                        .getResult();
        helper.invokeVirtual("addURL", "(Ljava/net/URL;)V", new Value[0], new Value[] {loader, url});
    }

}
