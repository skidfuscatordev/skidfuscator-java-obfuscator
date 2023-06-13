package dev.skidfuscator.core;

import dev.skidfuscator.jghost.GhostHelper;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.creator.SkidApplicationClassSource;
import dev.skidfuscator.obfuscator.creator.SkidFlowGraphDumper;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomResolvingJarDumper;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import dev.skidfuscator.obfuscator.verifier.Verifier;
import dev.skidfuscator.testclasses.TestRun;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.InnerClassNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarContents;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Shit code but heck I'm trying to speed code a test suite
 */
public class TestSkidfuscator extends Skidfuscator {
    private final Class<?>[] test;
    private final Consumer<List<Map.Entry<String, byte[]>>> callback;
    public TestSkidfuscator(Class<?>[] test, Consumer<List<Map.Entry<String, byte[]>>> callback) {
        super(SkidfuscatorSession
                .builder()
                .jmod(MiscUtil.isJmod())
                .analytics(false)
                .debug(true)
                .config(new File(TestSkidfuscator.class.getResource("/config/runtime.hocon").getFile()))
                .build());
        
        this.test = test;
        this.callback = callback;

        IntegerBlockPredicateRenderer.DEBUG = true;
    }

    public static boolean SKIP = false;
    public static boolean BEFORE_NATIVE = false;

    @Override
    protected void _importExempt() {
        LOGGER.post("Ignoring exempts since we're testing...");
    }

    @Override
    protected void _importJvm() {
        LOGGER.post("Beginning to import JVM...");
        if (!cached) {
            _cacheJvm(this);
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

        final Set<String> imported = new HashSet<>();
        final Queue<Class<?>> iterate = new LinkedBlockingDeque<>(Arrays.asList(test));
        for (final Class<?> clazz : iterate) {
            final ClassNode mapleNode;

            if (imported.contains(clazz.getName()))
                continue;

            imported.add(clazz.getName());

            try {
                mapleNode = ClassHelper.create(clazz.getName());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read runtime test class", e);
            }

            final SkidClassNode classNode = new SkidClassNode(mapleNode.node, this);
            final byte[] bytes = ClassHelper.toByteArray(classNode);

            final JarClassData data = new JarClassData(
                    classNode.getName() + ".class",
                    bytes,
                    classNode
            );

            if (classNode.node.innerClasses != null) {
                for (InnerClassNode innerClass : classNode.node.innerClasses) {
                    if (innerClass.name.startsWith("java"))
                        continue;
                    try {
                        final Class<?> subClazz = Class.forName(
                                innerClass.name.replace("/", ".")
                        );
                        if (imported.contains(subClazz.getName()))
                            continue;

                        iterate.add(subClazz);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            jarContents.getClassContents().add(data);
        }


        this.classSource = new SkidApplicationClassSource(
                "test source",
                true,
                jarContents,
                this
        );
        LOGGER.log("Finished importing classpath!");
    }

    @Override
    protected void _loadTransformer() {
        if (SKIP)
            return;

        super._loadTransformer();
    }

    @Override
    protected void _cleanup() {

    }

    @Override
    protected void _dump() {
        final PhantomResolvingJarDumper resolver = new PhantomResolvingJarDumper(
                this,
                jarContents,
                classSource
        );
        final ClassTree tree = this.getClassSource().getClassTree();
        final List<Map.Entry<String, byte[]>> dataMap = new ArrayList<>();

        final List<ClassNode> order = tree.getAllChildren(tree.getRootNode());
        Collections.reverse(order);
        order.stream()
                .filter(e -> this.classSource.isApplicationClass(e.getName()))
                .forEach(e -> {
                    Verifier.verify(e.node);

                    final AtomicReference<byte[]> bytes = new AtomicReference<>();
                    try {
                        final ClassWriter writer = resolver.buildClassWriter(
                                tree,
                                SkidFlowGraphDumper.TEST_COMPUTE
                                        ? ClassWriter.COMPUTE_MAXS
                                        : ClassWriter.COMPUTE_FRAMES
                        );
                        e.node.accept(writer); // must use custom writer which overrides getCommonSuperclass
                        bytes.set(writer.toByteArray());
                    } catch (Exception ex) {
                        ex.printStackTrace();

                        try {
                            final ClassWriter writer = resolver.buildClassWriter(tree, ClassWriter.COMPUTE_MAXS);
                            e.node.accept(writer); // must use custom writer which overrides getCommonSuperclass
                            bytes.set(writer.toByteArray());
                            System.err.println("\rFailed to write " + e.getName()
                                    + "! Writing with COMPUTE_MAXS, " +
                                    "which may cause runtime abnormalities\n");
                        } catch (Exception ex2) {
                            System.err.println("\rFailed to write " + e.getName()
                                    + "!");

                            bytes.set(jarContents.getClassContents().namedMap().get(e.getName() + ".class").getData());
                        }

                    }
                    dataMap.add(new Map.Entry<String, byte[]>() {
                        @Override
                        public String getKey() {
                            return e.getName();
                        }

                        @Override
                        public byte[] getValue() {
                            return bytes.get();
                        }

                        @Override
                        public byte[] setValue(byte[] value) {
                            throw new IllegalStateException("wtf");
                        }
                    });
                });

        callback.accept(dataMap);
    }

    private static boolean cached;
    private static final List<ApplicationClassSource> libs = new ArrayList<>();

    private static void _cacheJvm(final Skidfuscator skidfuscator) {
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
            libs.add(GhostHelper.getJvm(LOGGER, true, runtime, folder));
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

                libs.add(GhostHelper.getJvm(LOGGER, true, file, folder));
            }
            LOGGER.post("✓ Success");
        }

        final ClassNode classNode = ClassHelper.create(TestRun.class);
        final byte[] bytes = ClassHelper.toByteArray(classNode);
        final JarContents testContents = new JarContents();
        testContents.getClassContents().add(
                new JarClassData(
                        classNode.getName(),
                        bytes,
                        classNode
                )
        );

        libs.add(new SkidApplicationClassSource("test-classes", false, testContents, skidfuscator));

        LOGGER.log("Finished importing the JVM!");
        cached = true;
    }
}
