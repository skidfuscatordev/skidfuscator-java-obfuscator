package dev.skidfuscator.obf.init;

import com.google.common.collect.Streams;
import dev.skidfuscator.obf.utils.Counter;
import dev.skidfuscator.obf.yggdrasil.method.DefaultMethodInvokerResolver;
import dev.skidfuscator.obf.yggdrasil.method.MethodInvokerResolver;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.mapleir.DefaultInvocationResolver;
import org.mapleir.app.client.SimpleApplicationContext;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.AnalysisContext;
import org.mapleir.context.BasicAnalysisContext;
import org.mapleir.context.IRCache;
import org.mapleir.deob.PassGroup;
import org.mapleir.deob.dataflow.LiveDataFlowAnalysisImpl;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.topdank.byteio.in.SingleJarDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

@Getter
public class SkidSession {
    private static final Logger LOGGER = Logger.getLogger(SkidSession.class);
    private final ApplicationClassSource classSource;
    private final SingleJarDownloader<ClassNode> jarDownloader;
    private final IRCache irFactory;
    private final AnalysisContext cxt;
    //private final MethodInvokerResolver methodInvokerResolver;

    private final File outputFile;
    private final List<PassGroup> passes = new ArrayList<>();

    private final List<MethodNode> entryPoints = new ArrayList<>();

    private final Counter counter = new Counter();

    public SkidSession(ApplicationClassSource classSource, SingleJarDownloader<ClassNode> jarDownloader, File outputFile) {
        this.classSource = classSource;
        this.jarDownloader = jarDownloader;
        this.outputFile = outputFile;
        this.irFactory = new IRCache(ControlFlowGraphBuilder::build);
        this.cxt = new BasicAnalysisContext.BasicContextBuilder()
                .setApplication(classSource)
                .setInvocationResolver(new DefaultInvocationResolver(classSource))
                .setCache(irFactory)
                .setApplicationContext(new SimpleApplicationContext(classSource))
                .setDataFlowAnalysis(new LiveDataFlowAnalysisImpl(irFactory))
                .build();
        LOGGER.info("Iterating through " + cxt.getApplication().getClassTree().size() + " classes");
        Streams.stream(cxt.getApplication().iterate())
                .parallel()
                .filter(e -> this.getClassSource().isApplicationClass(e.getName()))
                .forEach(cn -> {
            cn.getMethods().forEach(m -> {
                try {
                    cxt.getIRCache().getFor(m);
                } catch (Exception e) {
                    LOGGER.error("Failed for class " + m.owner.getName() + "#" + m.getName() + m.getDesc(), e);
                }
                ;
            });
        });
        //this.methodInvokerResolver = new DefaultMethodInvokerResolver(this);
    }

    public void addPass(PassGroup pass) {
        this.passes.add(pass);
    }

    public void addEntryPoints(Collection<MethodNode> nodes) {
        this.entryPoints.addAll(nodes);
    }

    public void count() {
        this.counter.tick();
    }

    public int popCount() {
        final int count = counter.get();
        counter.reset();
        return count;
    }
}
