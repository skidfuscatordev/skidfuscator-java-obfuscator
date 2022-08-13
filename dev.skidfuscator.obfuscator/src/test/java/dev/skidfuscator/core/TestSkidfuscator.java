package dev.skidfuscator.core;

import dev.skidfuscator.jghost.GhostHelper;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.creator.SkidApplicationClassSource;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomResolvingJarDumper;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.ClassWriter;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarContents;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shit code but heck I'm trying to speed code a test suite
 */
public class TestSkidfuscator extends Skidfuscator {
    private final TestCase test;
    public TestSkidfuscator(TestCase test) {
        super(SkidfuscatorSession
                .builder()
                .jmod(MiscUtil.isJmod())
                .analytics(false)
                .build());
        this.test = test;
    }

    @Override
    protected void _importExempt() {
        LOGGER.post("Ignoring exempts since we're testing...");
    }

    @Override
    protected void _importJvm() {
        LOGGER.post("Beginning to import JVM...");
        if (!cached) {
            _cacheJvm();
        }

        for (ApplicationClassSource lib : libs) {
            this.classSource.addLibraries(new LibraryClassSource(
                    lib,
                    0
            ));
        }
        LOGGER.log("Finished importing JVM!");
    }

    @Override
    protected void _importClasspath() {
        LOGGER.post("Beginning to import classpath...");
        this.jarContents = new JarContents();
        this.classSource = new SkidApplicationClassSource(
                "test source",
                true,
                jarContents
        );

        for (Class<?> clazz : test.getClasses()) {
            final ClassNode classNode = ClassHelper.create(clazz);
            final byte[] bytes = ClassHelper.toByteArray(classNode);

            final JarClassData data = new JarClassData(
                    classNode.getName(),
                    bytes,
                    classNode
            );

            jarContents.getClassContents().add(data);
        }
        LOGGER.log("Finished importing classpath!");
    }

    @Override
    protected void _dump() {
        final PhantomResolvingJarDumper resolver = new PhantomResolvingJarDumper(
                this,
                jarContents,
                classSource
        );
        final ClassTree tree = this.getClassSource().getClassTree();
        final Map<String, byte[]> dataMap = new HashMap<>();

        for (ClassNode classNode : this.classSource.iterate()) {
            final ClassWriter writer = resolver.buildClassWriter(
                    tree,
                    ClassWriter.COMPUTE_FRAMES
            );
            classNode.node.accept(writer);

            dataMap.put(classNode.getName(), writer.toByteArray());
        }

        test.receiveAndExecute(dataMap);
    }

    private static boolean cached;
    private static final List<ApplicationClassSource> libs = new ArrayList<>();

    private static void _cacheJvm() {
        /* Import JVM */
        LOGGER.post("Beginning importing of the JVM...");

        final File folder = new File("mappings");

        final String home = System.getProperty("java.home");
        final File runtime = new File(
                home,
                MiscUtil.getJavaVersion() > 8
                        ? "jmods"
                        : "lib/rt.jar"
        );

        /*
         * Pardon my inverse condition, although the order will make sense in
         * a second. Before J9/J11, Java had all of its libraries compiled in
         * a single jar called rt.jar. This is no longer the case, although
         * since J8 is still the most predominantly used version of Java, it
         * is a no-brainer to support it.
         *
         * + I love J8,... death to "var" in Java
         */
        if (MiscUtil.getJavaVersion() < 9) {
            libs.add(GhostHelper.getJvm(true, runtime, folder));
            LOGGER.post("✓ Success");
        }
        /*
         * The choice of JMod in Java is so odd. Same "zip" format as other Jars,
         * but completely and utterly discoostin. Oh well whatever. Here we try
         * to download these fancily to be able to resolve all the classes in
         * what used to be rt.jar.
         */
        else {
            for (File file : runtime.listFiles()) {
                if (!file.getAbsolutePath().endsWith(".jmod"))
                    continue;

                libs.add(GhostHelper.getJvm(true, runtime, folder));
            }
            LOGGER.post("✓ Success");
        }
        LOGGER.log("Finished importing the JVM!");
        cached = true;
    }
}
