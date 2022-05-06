package dev.skidfuscator.obf.init;

import dev.skidfuscator.obf.SkidInstance;
import dev.skidfuscator.obf.SkidMethodRenderer;
import dev.skidfuscator.obf.phantom.PhantomJarDownloader;
import dev.skidfuscator.obf.utils.MapleJarUtil;
import dev.skidfuscator.obf.yggdrasil.EntryPoint;
import dev.skidfuscator.obf.yggdrasil.app.MapleEntryPoint;
import lombok.SneakyThrows;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.mapleir.ir.algorithms.SreedharDestructor;
import org.mapleir.ir.algorithms.TrollDestructor;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.codegen.ControlFlowGraphDumper;
import org.topdank.byteio.in.SingleJarDownloader;

import java.io.File;
import java.util.Map;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */
public class DefaultInitHandler implements InitHandler {
    @Override
    @SneakyThrows
    public SkidSession init(final SkidInstance instance) {
        System.out.println("Starting download of jar " + instance.getInput().getName() + "...");
        final PhantomJarDownloader<ClassNode> downloader = MapleJarUtil.importPhantomJar(instance.getInput());
        ApplicationClassSource classSource = new ApplicationClassSource(
                instance.getInput().getName(), downloader.getJarContents().getClassContents()
        );

        classSource.addLibraries(new LibraryClassSource(
                classSource,
                downloader.getPhantomContents().getClassContents()
        ));

        System.out.println("Starting download of runtime jar...");
        final SingleJarDownloader<ClassNode> libs = MapleJarUtil.importJar(instance.getRuntime());

        classSource.addLibraries(new LibraryClassSource(
                classSource,
                libs.getJarContents().getClassContents()
        ));

        if (instance.getLibs() != null && instance.getLibs().listFiles() != null) {
            for (File file : instance.getLibs().listFiles()) {
                // Really shitty hack
                if (file.getName().contains(".jar")) {
                    final SingleJarDownloader<ClassNode> lib = MapleJarUtil.importJar(file);

                    classSource.addLibraries(new LibraryClassSource(
                            classSource,
                            lib.getJarContents().getClassContents()
                    ));
                }
            }
        }

        final SkidSession session = new SkidSession(
                instance,
                classSource,
                downloader,
                instance.getOutput()
        );

        System.out.println("Evaluating classes...");

        for(Map.Entry<MethodNode, ControlFlowGraph> e : session.getCxt().getIRCache().entrySet()) {
            MethodNode mn = e.getKey();
            ControlFlowGraph cfg = e.getValue();
            try {
                cfg.verify();
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }

        final EntryPoint entryPoint = new MapleEntryPoint();

        session.addEntryPoints(entryPoint.getEntryPoints(
                session,
                classSource
        ));

        //session.getEntryPoints().forEach(e -> System.out.println(e.owner.node.name + "#" + e.node.name));


        //final ParameterResolver parameterResolver = new ZelixParameterTransformer().transform(session);
        new SkidMethodRenderer().render(session);

        for(Map.Entry<MethodNode, ControlFlowGraph> e : session.getCxt().getIRCache().entrySet()) {
            MethodNode mn = e.getKey();

            ControlFlowGraph cfg = e.getValue();

            try {
                cfg.verify();
            } catch (Exception ex){
                ex.printStackTrace();
            }
            (new ControlFlowGraphDumper(cfg, mn)).dump();
        }

        return session;
    }
}
