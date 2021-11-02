package dev.skidfuscator.obf.init;

import dev.skidfuscator.obf.SkidMethodRenderer;
import dev.skidfuscator.obf.utils.MapleJarUtil;
import dev.skidfuscator.obf.yggdrasil.EntryPoint;
import dev.skidfuscator.obf.yggdrasil.app.ApplicationEntryPoint;
import lombok.SneakyThrows;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
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
    public SkidSession init(final File jar, final File output) {
        System.out.println("Starting download of jar " + jar.getName() + "...");
        final SingleJarDownloader<ClassNode> downloader = MapleJarUtil.importJar(jar);
        ApplicationClassSource classSource = new ApplicationClassSource(
                jar.getName(), downloader.getJarContents().getClassContents()
        );

        System.out.println("Starting download of runtime jar...");
        final SingleJarDownloader<ClassNode> libs = MapleJarUtil.importJar(new File(System.getProperty("java.home"), "lib/rt.jar"));

        classSource.addLibraries(new LibraryClassSource(
                classSource,
                libs.getJarContents().getClassContents()
        ));

        final SkidSession session = new SkidSession(classSource, downloader, output);

        System.out.println("Evaluating classes...");


        for(Map.Entry<MethodNode, ControlFlowGraph> e : session.getCxt().getIRCache().entrySet()) {
            MethodNode mn = e.getKey();
            ControlFlowGraph cfg = e.getValue();
            try {
                cfg.verify();
            } catch (Exception ex){
                ex.printStackTrace();
            }


            BoissinotDestructor.leaveSSA(cfg);
            LocalsReallocator.realloc(cfg);

            try {
                cfg.verify();
            } catch (Exception ex){
                ex.printStackTrace();
            }

        }

        final EntryPoint entryPoint = new ApplicationEntryPoint();

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
