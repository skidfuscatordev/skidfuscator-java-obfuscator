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
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomJarDownloader;
import dev.skidfuscator.obfuscator.predicate.PredicateAnalysis;
import dev.skidfuscator.obfuscator.predicate.SimplePredicateAnalysis;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFactory;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.impl.IntegerBlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.impl.IntegerClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.impl.IntegerMethodOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.resolver.SkidInvocationResolver;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.impl.NegationTransformer;
import dev.skidfuscator.obfuscator.transform.impl.SwitchTransformer;
import dev.skidfuscator.obfuscator.transform.impl.flow.BasicConditionTransformer;
import dev.skidfuscator.obfuscator.transform.impl.flow.BasicExceptionTransformer;
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
import org.mapleir.app.service.LocateableClassNode;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.AnalysisContext;
import org.mapleir.context.BasicAnalysisContext;
import org.mapleir.context.IRCache;
import org.mapleir.deob.PassGroup;
import org.mapleir.deob.dataflow.LiveDataFlowAnalysisImpl;
import org.mapleir.ir.cfg.ControlFlowGraph;
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

@Getter
public class Skidfuscator {
    public static final TimedLogger LOGGER = new TimedLogger(LogManager.getLogger(Skidfuscator.class));

    private final SkidfuscatorSession session;

    private SkidApplicationClassSource classSource;
    private LibraryClassSource jvmClassSource;
    private AbstractJarDownloader<ClassNode> jarDownloader;
    private IRCache irFactory;
    private AnalysisContext cxt;

    private Hierarchy hierarchy;

    private OrderAnalysis orderAnalysis;
    private ExemptAnalysis exemptAnalysis;
    private PredicateAnalysis predicateAnalysis;

    private final Counter counter = new Counter();

    public Skidfuscator(SkidfuscatorSession session) {
        this.session = session;
    }

    public void run() {
        LOGGER.post("Beginning Skidfuscator Enterprise...");
        SkiddedDirectory.init(null);
        this.irFactory = new SkidCache(this);
        this.exemptAnalysis = new SimpleExemptAnalysis();

        LOGGER.post("Resolving predicate analysis...");

        this.predicateAnalysis = new SimplePredicateAnalysis.Builder()
                .skidfuscator(this)
                .blockOpaqueFactory(new PredicateFactory<BlockOpaquePredicate, SkidMethodNode>() {
                    @Override
                    public BlockOpaquePredicate build(SkidMethodNode methodNode) {
                        return new IntegerBlockOpaquePredicate();
                    }
                })
                .methodOpaqueFactory(new PredicateFactory<MethodOpaquePredicate, SkidGroup>() {
                    @Override
                    public MethodOpaquePredicate build(SkidGroup group) {
                        return new IntegerMethodOpaquePredicate(group);
                    }
                })
                .classOpaqueFactory(new PredicateFactory<ClassOpaquePredicate, SkidClassNode>() {
                    @Override
                    public ClassOpaquePredicate build(SkidClassNode classNode) {
                        return new IntegerClassOpaquePredicate(classNode);
                    }
                })
                .classStaticOpaqueFactory(new PredicateFactory<ClassOpaquePredicate, SkidClassNode>() {
                    @Override
                    public ClassOpaquePredicate build(SkidClassNode skidClassNode) {
                        return new IntegerClassOpaquePredicate(skidClassNode);
                    }
                })
                .build();

        LOGGER.log("Finished resolving predicate analysis!");

        /* Importation and exemptions */
        LOGGER.post("Importing exemptions...");
        if (session.getExempt() != null) {
            try  {
                final List<String> exclusions = new ArrayList<>();

                final FileReader fileReader = new FileReader(session.getExempt());
                final BufferedReader br = new BufferedReader(fileReader);
                String exclusion;
                while ((exclusion = br.readLine()) != null) {
                    exclusions.add(exclusion);
                }

                try(ProgressBar progressBar = ProgressUtil.progress(exclusions.size())) {
                    for (String s : exclusions) {
                        exemptAnalysis.add(s);
                        progressBar.step();
                    }
                }
            }
            catch (IOException ex) {
                LOGGER.error("Error reading exclusions file", ex);
                System.exit(1);
            }
        }
        LOGGER.log("Finished importing exemptions");


        LOGGER.post("Importing jar...");
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

        if (session.getLibs() != null && session.getLibs().listFiles() != null) {
            final File[] libs = session.getLibs().listFiles();
            LOGGER.post("Importing " + libs.length + " libs...");
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

                final LocateableClassNode classNode = libraryClassSource.findClass("org/json/simple/parser/ParseException");
                if (classNode == null) {
                    System.err.println("FUCK");
                    for (ClassNode vertex : libraryClassSource.iterate()) {
                        System.out.println(vertex.getDisplayName());
                    }
                }

                LOGGER.log("Finished importing libs!");
            } catch (Throwable e) {
                /* Failed to load libs as a whole */
                LOGGER.error("Failed to load libs at path " + session.getLibs().getAbsolutePath(), e);
            }
        }

        /* Add phantom libs for any content / links which arent generated (low priority) */
        this.classSource.addLibraries(new LibraryClassSource(
                new ApplicationClassSource(
                        "phantom",
                        downloader.getPhantomContents().getClassContents().stream().map(JarClassData::getClassNode).collect(Collectors.toList())
                ),
                -1
        ));
        LOGGER.log("Finished importing classpath!");

        /* Import JVM */
        LOGGER.post("Beginning importing of the JVM...");
        if (!session.isJmod()) {
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
        } else {
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
                new BasicConditionTransformer(this),
                new BasicExceptionTransformer(this),
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

    interface Caller {
        SkidTransformEvent callBase();

        ClassTransformEvent callClass(final SkidClassNode classNode);

        GroupTransformEvent callGroup(final SkidGroup group);

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

