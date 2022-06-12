package dev.skidfuscator.obfuscator;

import dev.skidfuscator.obfuscator.creator.SkidApplicationClassSource;
import dev.skidfuscator.obfuscator.creator.SkidCache;
import dev.skidfuscator.obfuscator.creator.SkidFlowGraphDumper;
import dev.skidfuscator.obfuscator.directory.SkiddedDirectory;
import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.Listener;
import dev.skidfuscator.obfuscator.event.impl.transform.ClassTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.GroupTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.MethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.SkidTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.clazz.*;
import dev.skidfuscator.obfuscator.event.impl.transform.group.*;
import dev.skidfuscator.obfuscator.event.impl.transform.method.*;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.*;
import dev.skidfuscator.obfuscator.exempt.ExemptAnalysis;
import dev.skidfuscator.obfuscator.exempt.SimpleExemptAnalysis;
import dev.skidfuscator.obfuscator.hierarchy.Hierarchy;
import dev.skidfuscator.obfuscator.hierarchy.SkidHierarchy;
import dev.skidfuscator.obfuscator.order.OrderAnalysis;
import dev.skidfuscator.obfuscator.phantom.jghost.GhostHelper;
import dev.skidfuscator.obfuscator.phantom.jghost.tree.GhostLibrary;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomJarDownloader;
import dev.skidfuscator.obfuscator.predicate.PredicateAnalysis;
import dev.skidfuscator.obfuscator.predicate.SimplePredicateAnalysis;
import dev.skidfuscator.obfuscator.predicate.opaque.impl.IntegerBlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.impl.IntegerClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.impl.IntegerMethodOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.resolver.SkidInvocationResolver;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.impl.SwitchTransformer;
import dev.skidfuscator.obfuscator.transform.impl.flow.*;
import dev.skidfuscator.obfuscator.transform.impl.misc.AhegaoTransformer;
import dev.skidfuscator.obfuscator.transform.impl.number.NumberTransformer;
import dev.skidfuscator.obfuscator.transform.impl.string.StringTransformer;
import dev.skidfuscator.obfuscator.util.MapleJarUtil;
import dev.skidfuscator.obfuscator.util.ProgressUtil;
import dev.skidfuscator.obfuscator.util.misc.Counter;
import dev.skidfuscator.obfuscator.util.misc.TimedLogger;
import lombok.Getter;
import me.tongfei.progressbar.ProgressBar;
import org.apache.log4j.LogManager;
import org.mapleir.app.client.SimpleApplicationContext;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.AnalysisContext;
import org.mapleir.context.BasicAnalysisContext;
import org.mapleir.context.IRCache;
import org.mapleir.deob.PassGroup;
import org.mapleir.deob.dataflow.LiveDataFlowAnalysisImpl;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteio.in.AbstractJarDownloader;
import org.topdank.byteio.in.SingleJarDownloader;
import org.topdank.byteio.in.SingleJmodDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Skidfuscator.
 */
@Getter
public class Skidfuscator {
    public static final TimedLogger LOGGER = new TimedLogger(LogManager.getLogger(Skidfuscator.class));
    public static final int ASM_VERSION = Opcodes.ASM9;

    private final SkidfuscatorSession session;

    private SkidApplicationClassSource classSource;
    private LibraryClassSource jvmClassSource;
    private AbstractJarDownloader<ClassNode> jarDownloader;
    private SkidCache irFactory;
    private AnalysisContext cxt;

    private Hierarchy hierarchy;

    private OrderAnalysis orderAnalysis;
    private ExemptAnalysis exemptAnalysis;
    private PredicateAnalysis predicateAnalysis;

    private final Counter counter = new Counter();

    /**
     * Instantiates a new Skidfuscator.
     *
     * @param session the session
     */
    public Skidfuscator(SkidfuscatorSession session) {
        this.session = session;
    }

    /**
     * Runs the execution of the obfuscator.
     */
    public void run() {
        LOGGER.post("Beginning Skidfuscator Enterprise...");

        /*
         * Initializes a null skid directory. This skid directory is used as a
         * cache or a temporary directory, most often for silly things such as
         * JPhantom or in the near future as a cache for the Ghost pre-computed
         * mappings.
         */
        SkiddedDirectory.init(null);

        /*
         * Here is initialized both the exempt analysis and the skid cache.
         *
         * The SkidCache is an extension of MapleIR's IRCache
         * The ExemptAnalysis is a manager which handles exemptions
         */
        this.irFactory = new SkidCache(this);
        this.exemptAnalysis = new SimpleExemptAnalysis();

        /*
         * Here we initialize our opaque predicate type. As of right now
         * only one has been completed: the direct integer opaque predicate.
         * In the future, it will be possible to add compatibility for other
         * types such as longs, byte arrays etc...
         */
        LOGGER.post("Resolving predicate analysis...");
        this.predicateAnalysis = new SimplePredicateAnalysis.Builder()
                .skidfuscator(this)
                /*
                 * The BlockOpaqueFactory is the factory which builds opaque
                 * predicates used in the flow obfuscation (hence why 'block').
                 *
                 * These are directly present at every BasicBlock in the CFG
                 */
                .blockOpaqueFactory(IntegerBlockOpaquePredicate::new)

                /*
                 * The MethodOpaqueFactory is the factory which builds
                 * opaque predicates used to call methods. Each method
                 * has two opaque predicates:
                 *
                 * Public predicate:    The one used to call the method
                 * Private predicate:   The one used inside the method
                 *                      itself, usually an extension
                 *                      or transformation of the public
                 *                      one
                 *
                 */
                .methodOpaqueFactory(IntegerMethodOpaquePredicate::new)

                /*
                 * The ClassOpaqueFactory is the factory used to build
                 * predicates present at the Object instance level. This
                 * predicate can be used in any non-static method and is
                 * initiated during the <init> clause or the instance init
                 * through fields.
                 */
                .classOpaqueFactory(IntegerClassOpaquePredicate::new)

                /*
                 * The ClassStaticOpaqueFactory is the factory used to build
                 * the predicates present at the static class level. These
                 * predicates are used for any static methods or can sometimes
                 * be paired to be used with non-static, however at a loss
                 * of strength due to the predominant less-secure nature
                 * as it can be more easily emulated
                 */
                .classStaticOpaqueFactory(IntegerClassOpaquePredicate::new)

                /* Builder */
                .build();
        LOGGER.log("Finished resolving predicate analysis!");

        /* Importation and exemptions */
        LOGGER.post("Importing exemptions...");
        if (session.getExempt() != null) {
            try {
                final List<String> exclusions = new ArrayList<>();

                /*
                 * This method is really scuffed but temporary for now. As
                 * of right now we read every line from a .txt file, cache
                 * them into an array list then pass them off to parsing.
                 */
                final FileReader fileReader = new FileReader(session.getExempt());
                final BufferedReader br = new BufferedReader(fileReader);
                String exclusion;
                while ((exclusion = br.readLine()) != null) {
                    exclusions.add(exclusion);
                }

                /*
                 * This is the parsing bit. We initiate a progress bar and
                 * simply just call the exempt analysis which builds the
                 * exclusion call and caches it.
                 */
                try(ProgressBar progressBar = ProgressUtil.progress(exclusions.size())) {
                    for (String s : exclusions) {
                        exemptAnalysis.add(s);
                        progressBar.step();
                    }
                }
            } catch (IOException ex) {
                /*
                 * If there's any error, it can pose significant issues with
                 * Skidfuscator. It's best to exit the program as of now.
                 *
                 * TODO:    Add better syntax highlighting for issues/parsing
                 *          failure or exceptions
                 */
                LOGGER.error("Error reading exclusions file", ex);
                System.exit(1);
            }
        }
        LOGGER.log("Finished importing exemptions");


        LOGGER.post("Importing jar...");
        /*
         * This is the main jar download. We'll be keeping a cache of the jar
         * download as it will simplify our output methods. In several cases,
         * many jars have classes with names that do not align with their
         * respective ClassNode#getName, causing conflicts, hence why the cache
         * of the jar downloader.
         */
        final PhantomJarDownloader<ClassNode> downloader = MapleJarUtil.importPhantomJar(
                session.getInput(),
                this
        );
        this.jarDownloader = downloader;
        this.classSource = new SkidApplicationClassSource(
                session.getInput().getName(),
                downloader.getJarContents()
        );
        LOGGER.log("Finished importing jar.");

        /*
         * Caching the libs is a fucking disaster - and I apologize
         * As of right now for anyone who may be trying to read this.
         * Currently, we have to re-cache entire class files and
         * repeatedly read over and over again thousands of class files
         * for extremely minimal information.
         *
         * Soon - hopefully - I'll create a mapping format or nick it
         * from an OS project and convert libs to this mapping format,
         * then cache the libs md5 and sha1/sha256 hashes, alongside
         * any maven packaging. My objective is to be able to host an
         * entire database of mappings in sub 1Gb of storage to allow
         * for all of Skidfuscator Enterprise to be hosted on the cloud.
         *
         * This would allow for a lot of cool stuff, including tracking
         * and remote HWIDs.
         */
        if (session.getLibs() != null && session.getLibs().listFiles() != null) {
            final File[] libs = Arrays.stream(session.getLibs().listFiles())
                    .filter(e -> e.getAbsolutePath().endsWith(".jar"))
                    .toArray(File[]::new);

            LOGGER.post("Importing " + libs.length + " libs...");

            for (File lib : libs) {
                LOGGER.post("[+] " + lib.getAbsolutePath());

                final GhostLibrary library = GhostHelper.createFromLibraryFile(lib);
                final File output = new File(
                        session.getLibs().getParent(), "mappings/" + lib.getName() + ".json"
                );
                output.getParentFile().mkdirs();

                GhostHelper.saveLibraryFile(library, output);
            }

            try {
                /* Download the libraries jar contents */
                final AbstractJarDownloader<ClassNode> jar = MapleJarUtil.importJars(libs);

                /* Create a new library class source with superior to default priority */
                final ApplicationClassSource libraryClassSource = new ApplicationClassSource(
                        "libraries",
                        jar.getJarContents().getClassContents().stream().map(JarClassData::getClassNode).collect(Collectors.toList())
                );
                LOGGER.post("Imported " + jar.getJarContents().getClassContents().size() + " library classes...");

                /* Add library source to class source */
                classSource.addLibraries(new LibraryClassSource(
                        libraryClassSource,
                        5
                ));
                LOGGER.log("Finished importing libs!");
            } catch (Throwable e) {
                /* Failed to load libs as a whole */
                LOGGER.error("Failed to load libs at path " + session.getLibs().getAbsolutePath(), e);
            }
        }

        /*
         * Exclusively run this if JPhantom is activated. JPhantom computes some
         * stuff really well, albeit it just necks itself the moment the software
         * grows in size.
         *
         * Furthermore, since it computes classes which could be present in other
         * libraries, we set the priority to -1, making it the last fallback result.
         */
        if (session.isPhantom()) {
            this.classSource.addLibraries(new LibraryClassSource(
                    new ApplicationClassSource(
                            "phantom",
                            downloader.getPhantomContents()
                                    .getClassContents()
                                    .stream()
                                    .map(JarClassData::getClassNode)
                                    .collect(Collectors.toList())
                    ),
                    -1
            ));
        }
        LOGGER.log("Finished importing classpath!");

        /* Import JVM */
        LOGGER.post("Beginning importing of the JVM...");

        /*
         * Pardon my inverse condition, although the order will make sense in
         * a second. Before J9/J11, Java had all of its libraries compiled in
         * a single jar called rt.jar. This is no longer the case, although
         * since J8 is still the most predominantly used version of Java, it
         * is a no-brainer to support it.
         *
         * + I love J8,... death to "var" in Java
         */
        if (!session.isJmod()) {
            LOGGER.post("↳ Trying to download " + session.getRuntime().toString());
            final SingleJarDownloader<ClassNode> libs = MapleJarUtil.importJar(
                    session.getRuntime()
            );
            this.classSource.addLibraries((jvmClassSource = new LibraryClassSource(
                    new ApplicationClassSource(
                            "runtime",
                            libs.getJarContents()
                                    .getClassContents()
                                    .stream()
                                    .map(JarClassData::getClassNode)
                                    .collect(Collectors.toList())
                    ),
                    0
            )));
            LOGGER.post("✓ Success");
        }
        /*
         * The choice of JMod in Java is so odd. Same "zip" format as other Jars,
         * but completely and utterly discoostin. Oh well whatever. Here we try
         * to download these fancily to be able to resolve all the classes in
         * what used to be rt.jar.
         */
        else {
            for (File file : session.getRuntime().listFiles()) {
                if (!file.getAbsolutePath().endsWith(".jmod"))
                    continue;
                LOGGER.post("↳ Trying to download " + file.toString());
                final SingleJmodDownloader<ClassNode> libs = MapleJarUtil.importJmod(
                        file
                );
                this.classSource.addLibraries((jvmClassSource = new LibraryClassSource(
                        new ApplicationClassSource(
                                file.getName(),
                                libs.getJarContents().getClassContents()
                                        .stream()
                                        .map(JarClassData::getClassNode)
                                        .collect(Collectors.toList())
                        ),
                        0
                )));
                LOGGER.post("✓ Success");
            }

        }
        LOGGER.log("Finished importing the JVM!");

        /* Resolve context */
        LOGGER.post("Resolving basic context...");
        this.cxt = new BasicAnalysisContext.BasicContextBuilder()
                .setApplication(classSource)
                .setInvocationResolver(new SkidInvocationResolver(classSource))
                .setCache(irFactory)
                .setApplicationContext(new SimpleApplicationContext(classSource))
                .setDataFlowAnalysis(new LiveDataFlowAnalysisImpl(irFactory))
                .build();
        LOGGER.log("Finished resolving basic context!");

        /* Resolve hierarchy */
        LOGGER.post("Resolving hierarchy (this could take a while)...");
        this.hierarchy = new SkidHierarchy(this);
        this.hierarchy.cache();
        LOGGER.log("Finished resolving hierarchy!");

        /* Register opaque predicate renderer and transformers */
        LOGGER.post("Loading transformers...");
        EventBus.register(new IntegerBlockPredicateRenderer(this, null));

        /*
         * VAZIAK
         *
         * MINOR CHANGES
         *
         * Here though shall puteth all your transformers. Enjoy!
         */
        for (Listener o : Arrays.asList(
                new StringTransformer(this),
                //new NegationTransformer(this),
                //new FlatteningFlowTransformer(this),
                new NumberTransformer(this),
                new SwitchTransformer(this),
                new BasicSimplifierTransformer(this),
                new BasicConditionTransformer(this),
                new BasicExceptionTransformer(this),
                new BasicRangeTransformer(this),
                new AhegaoTransformer(this)
                //
                //new FactoryMakerTransformer()
        )) {
            EventBus.register(o);
        }
        LOGGER.log("Finished loading transformers...");
        LOGGER.post("Executing transformers...");

        init();
        preTransform();
        transform();
        postTransform();
        finalTransform();
        LOGGER.log("Finished executing transformers...");

        LOGGER.post("Dumping classes...");
        try(ProgressBar progressBar = ProgressUtil.progress(cxt.getIRCache().size())) {
            for(Map.Entry<MethodNode, ControlFlowGraph> e : new HashSet<>(cxt.getIRCache().entrySet())) {
                MethodNode mn = e.getKey();
                ControlFlowGraph cfg = e.getValue();

                try {
                    cfg.verify();
                    (new SkidFlowGraphDumper(this, cfg, mn)).dump();
                } catch (Exception ex){
                    if (ex instanceof IllegalStateException) {
                        throw ex;
                    }
                    ex.printStackTrace();
                }
                progressBar.step();
            }
        }
        LOGGER.log("Finished dumping classes...");

        LOGGER.post("Dumping jar...");
        EventBus.end();
        try {
            MapleJarUtil.dumpJar(
                    this,
                    new PassGroup("Output"),
                    session.getOutput().getPath()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.log("Finished dumping jar...");
        LOGGER.post("Goodbye!");
    }

    /**
     * The interface Caller.
     */
    interface Caller {
        /**
         * Call base skid transform event.
         *
         * @return the skid transform event
         */
        SkidTransformEvent callBase();

        /**
         * Call class class transform event.
         *
         * @param classNode the class node
         * @return the class transform event
         */
        ClassTransformEvent callClass(final SkidClassNode classNode);

        /**
         * Call group group transform event.
         *
         * @param group the group
         * @return the group transform event
         */
        GroupTransformEvent callGroup(final SkidGroup group);

        /**
         * Call method method transform event.
         *
         * @param methodNode the method node
         * @return the method transform event
         */
        MethodTransformEvent callMethod(final SkidMethodNode methodNode);
    }

    private void run(final Caller caller) {
        EventBus.call(caller.callBase());

        try (ProgressBar progressBar = ProgressUtil.progress(hierarchy.getClasses().size())){
            for (ClassNode ccls : hierarchy.getClasses()) {
                final SkidClassNode classNode = (SkidClassNode) ccls;

                if (exemptAnalysis.isExempt(classNode)) {
                    progressBar.step();
                    continue;
                }

                EventBus.call(caller.callClass(classNode));
                progressBar.step();
            }
        }

        try (ProgressBar progressBar = ProgressUtil.progress(hierarchy.getGroups().size())){
            for (SkidGroup group : hierarchy.getGroups()) {
                if (group.getMethodNodeList().stream().anyMatch(e -> exemptAnalysis.isExempt(e))) {
                    progressBar.step();
                    continue;
                }

                EventBus.call(caller.callGroup(group));
                progressBar.step();
            }
        }

        final int size = hierarchy.getClasses().stream().mapToInt(e -> e.getMethods().size()).sum();

        try (ProgressBar progressBar = ProgressUtil.progress(size)){
            for (ClassNode ccls : hierarchy.getClasses()) {
                final SkidClassNode classNode = (SkidClassNode) ccls;

                if (exemptAnalysis.isExempt(classNode)) {
                    progressBar.stepBy(classNode.getMethods().size());
                    continue;
                }

                for (MethodNode cmth : classNode.getMethods()) {
                    final SkidMethodNode methodNode = (SkidMethodNode) cmth;

                    if (methodNode.isAbstract() || methodNode.isNative()) {
                        progressBar.step();
                        continue;
                    }

                    if (exemptAnalysis.isExempt(methodNode)) {
                        progressBar.step();
                        continue;
                    }

                    EventBus.call(caller.callMethod(methodNode));
                    progressBar.step();
                }
            }
        }
    }

    private void init() {
        final Skidfuscator skidfuscator = this;

        run(new Caller() {
            @Override
            public SkidTransformEvent callBase() {
                return new InitSkidTransformEvent(skidfuscator);
            }

            @Override
            public ClassTransformEvent callClass(SkidClassNode classNode) {
                return new InitClassTransformEvent(skidfuscator, classNode);
            }

            @Override
            public GroupTransformEvent callGroup(SkidGroup group) {
                return new InitGroupTransformEvent(skidfuscator, group);
            }

            @Override
            public MethodTransformEvent callMethod(SkidMethodNode methodNode) {
                return new InitMethodTransformEvent(skidfuscator, methodNode);
            }
        });
    }

    private void preTransform() {
        final Skidfuscator skidfuscator = this;

        run(new Caller() {
            @Override
            public SkidTransformEvent callBase() {
                return new PreSkidTransformEvent(skidfuscator);
            }

            @Override
            public ClassTransformEvent callClass(SkidClassNode classNode) {
                return new PreClassTransformEvent(skidfuscator, classNode);
            }

            @Override
            public GroupTransformEvent callGroup(SkidGroup group) {
                return new PreGroupTransformEvent(skidfuscator, group);
            }

            @Override
            public MethodTransformEvent callMethod(SkidMethodNode methodNode) {
                return new PreMethodTransformEvent(skidfuscator, methodNode);
            }
        });
    }

    private void transform() {
        final Skidfuscator skidfuscator = this;

        run(new Caller() {
            @Override
            public SkidTransformEvent callBase() {
                return new RunSkidTransformEvent(skidfuscator);
            }

            @Override
            public ClassTransformEvent callClass(SkidClassNode classNode) {
                return new RunClassTransformEvent(skidfuscator, classNode);
            }

            @Override
            public GroupTransformEvent callGroup(SkidGroup group) {
                return new RunGroupTransformEvent(skidfuscator, group);
            }

            @Override
            public MethodTransformEvent callMethod(SkidMethodNode methodNode) {
                return new RunMethodTransformEvent(skidfuscator, methodNode);
            }
        });
    }

    private void postTransform() {
        final Skidfuscator skidfuscator = this;

        run(new Caller() {
            @Override
            public SkidTransformEvent callBase() {
                return new PostSkidTransformEvent(skidfuscator);
            }

            @Override
            public ClassTransformEvent callClass(SkidClassNode classNode) {
                return new PostClassTransformEvent(skidfuscator, classNode);
            }

            @Override
            public GroupTransformEvent callGroup(SkidGroup group) {
                return new PostGroupTransformEvent(skidfuscator, group);
            }

            @Override
            public MethodTransformEvent callMethod(SkidMethodNode methodNode) {
                return new PostMethodTransformEvent(skidfuscator, methodNode);
            }
        });
    }

    private void finalTransform() {
        final Skidfuscator skidfuscator = this;

        run(new Caller() {
            @Override
            public SkidTransformEvent callBase() {
                return new FinalSkidTransformEvent(skidfuscator);
            }

            @Override
            public ClassTransformEvent callClass(SkidClassNode classNode) {
                return new FinalClassTransformEvent(skidfuscator, classNode);
            }

            @Override
            public GroupTransformEvent callGroup(SkidGroup group) {
                return new FinalGroupTransformEvent(skidfuscator, group);
            }

            @Override
            public MethodTransformEvent callMethod(SkidMethodNode methodNode) {
                return new FinalMethodTransformEvent(skidfuscator, methodNode);
            }
        });
    }
}

