package dev.skidfuscator.core;

import dev.skidfuscator.core.classloader.SkidClassLoader;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomResolvingJarDumper;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.util.MapleJarUtil;
import dev.skidfuscator.testclasses.TestRun;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapleir.asm.ClassHelper;
import org.mapleir.deob.PassGroup;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarContents;
import org.topdank.byteengineer.commons.data.JarResource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public abstract class SkidTest implements TestCase {
    protected Skidfuscator skidfuscator;
    @Test
    public void test() {
        final URL[] urls = new URL[0];

        try(SkidClassLoader classLoader = new SkidClassLoader(urls)) {
            final Class<?> clazz = classLoader.loadClass(this.getMainClass().getName());
            final TestRun run = (TestRun) clazz.newInstance();
            run.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed execution", e);
        }

        this.skidfuscator = new TestSkidfuscator(
                this.getClasses(),
                this::receiveAndExecute
        );
        skidfuscator.run();
    }

    @Override
    public void receiveAndExecute(List<Map.Entry<String, byte[]>> output) {
        final URL[] urls = new URL[0];

        try(SkidClassLoader classLoader = new SkidClassLoader(urls)) {
            for (Map.Entry<String, byte[]> entry : output) {
                final String name = entry.getKey();
                final byte[] clazzData = entry.getValue();

                classLoader.defineClass(name, clazzData);
            }

            final Class<?> clazz = classLoader.loadClass(this.getMainClass().getName());
            final TestRun run = (TestRun) clazz.newInstance();
            run.run();

            if (TestSkidfuscator.SKIP) {
                Assertions.fail("Transformers are causing the issue!");
            }
        } catch (Throwable e) {
            System.out.println("------");
            e.printStackTrace();
            System.out.println("------");

            try {
                skidfuscator.getJarContents().getClassContents().add(
                        new JarClassData(
                                "dev/skidfuscator/Bootstrap.class",
                                new byte[0],
                                generateBootstrap()
                        )
                );

                final org.mapleir.asm.ClassNode classNode = ClassHelper.create(TestRun.class);
                skidfuscator.getJarContents().getClassContents().add(
                        new JarClassData(
                                "dev/skidfuscator/testclasses/TestRun.class",
                                new byte[0],
                                classNode
                        )
                );

                final Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(
                        Attributes.Name.MANIFEST_VERSION,
                        "1.1"
                );
                manifest.getMainAttributes().put(
                        Attributes.Name.MAIN_CLASS,
                        "dev.skidfuscator.Bootstrap"
                );

                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                manifest.write(outputStream);
                skidfuscator.getJarContents().getResourceContents().add(
                        new JarResource(
                                "META-INF/MANIFEST.MF",
                                outputStream.toByteArray()
                        )
                );
                MapleJarUtil.dumpJar(
                        skidfuscator,
                        new PassGroup("Output"),
                        TestSkidfuscator.SKIP ? "dump.jar" : "dump-transformers.jar"
                );

                if (!TestSkidfuscator.SKIP) {
                    TestSkidfuscator.SKIP = true;

                    test();
                } else {
                    TestSkidfuscator.SKIP = false;
                }

            } catch (IOException ie) {
                ie.printStackTrace();
            }

            throw new IllegalStateException("Failed execution: " + e.getMessage(), e);
        }
    }

    private SkidClassNode generateBootstrap() {
        final ClassNode classNode = new ClassNode();
        classNode.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                "dev/skidfuscator/Bootstrap",
                null,
                "java/lang/Object",
                new String[0]
        );

        final SkidClassNode skidClassNode = new SkidClassNode(
                classNode,
                skidfuscator
        );

        final SkidMethodNode methodNode = skidClassNode.createMethod()
                .access(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
                .name("main")
                .desc("([Ljava/lang/String;)V")
                .exceptions(new String[0])
                .phantom(true)
                .signature(null)
                .build();
        final String parent = this.getMainClass().getName().replace(".", "/");

        final BasicBlock entry = methodNode.getEntryBlock();
        entry.add(new PopStmt(
                new VirtualInvocationExpr(
                        InvocationExpr.CallType.VIRTUAL,
                        new Expr[]{
                                new InitialisedObjectExpr(
                                        parent,
                                        "()V",
                                        new Expr[0]
                                )
                        },
                        parent,
                        "run",
                        "()V"
                )
        ));
        entry.add(new ReturnStmt());
        methodNode.dump();

        return skidClassNode;
    }

}