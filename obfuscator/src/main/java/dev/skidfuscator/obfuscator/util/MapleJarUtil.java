package dev.skidfuscator.obfuscator.util;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.SkidASMFactory;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomJarDownloader;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomResolvingJarDumper;
import lombok.SneakyThrows;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.deob.PassGroup;
import org.objectweb.asm.ClassWriter;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteio.in.MultiJarDownloader;
import org.topdank.byteio.in.SingleJarDownloader;
import org.topdank.byteio.in.SingleJmodDownloader;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * @author Ghast
 * @since 12/12/2020
 * HideMySkewnessCheckObfuscator © 2020
 */
public class MapleJarUtil {
    public static void dumpJar(Skidfuscator skidfuscator, PassGroup masterGroup, String outputFile) throws IOException {
        (new PhantomResolvingJarDumper(skidfuscator, skidfuscator.getJarDownloader().getJarContents(), skidfuscator.getClassSource()) {

            @Override
            public int dumpClass(JarOutputStream out, JarClassData classData) throws IOException {
                ClassNode cn = classData.getClassNode();
                JarEntry entry = new JarEntry(classData.getName());

                if (cn.isAnnoyingVersion()) {
                    final JarClassData resource = skidfuscator
                            .getJarDownloader()
                            .getJarContents()
                            .getClassContents()
                            .namedMap()
                            .get(classData.getName());

                    if (resource == null) {
                        throw new IllegalStateException("Failed to find class source for " + cn.getName());
                    }
                    out.putNextEntry(entry);
                    out.write(resource.getData());
                    return 1;
                }

                ClassTree tree = skidfuscator.getClassSource().getClassTree();

                for (MethodNode m : cn.getMethods()) {
                    if (m.node.instructions.size() > 10000) {
                        System.out.println("\rlarge method: " + m + " @" + m.node.instructions.size() + "\n");
                    }
                }

                try {
                    out.putNextEntry(entry);
                    try {
                        ClassWriter writer = this.buildClassWriter(tree, ClassWriter.COMPUTE_FRAMES);
                        cn.node.accept(writer);
                        out.write(writer.toByteArray());
                    } catch (Exception var8) {
                        ClassWriter writer = this.buildClassWriter(tree, ClassWriter.COMPUTE_MAXS);
                        cn.node.accept(writer);
                        out.write(writer.toByteArray());
                        var8.printStackTrace();
                        System.err.println("\rFailed to write " + cn.getName() + "! Writing with COMPUTE_MAXS, which may cause runtime abnormalities\n");
                    }
                } catch (Exception var9) {
                    System.err.println("\rFailed to write " + cn.getName() + "! Skipping class...\n");
                    var9.printStackTrace();
                }

                return 1;
            }
        }).dump(new File(outputFile));
    }

    @SneakyThrows
    public static MultiJarDownloader<ClassNode> importJars(File... file) {
        final JarInfo[] jarInfos = new JarInfo[file.length];

        for (int i = 0; i < file.length; i++) {
            jarInfos[i] = new JarInfo(file[i]);
        }

        MultiJarDownloader<ClassNode> dl = new MultiJarDownloader<>(jarInfos);
        dl.download();

        return dl;
    }

    @SneakyThrows
    public static SingleJarDownloader<ClassNode> importJar(File file) {
        SingleJarDownloader<ClassNode> dl = new SingleJarDownloader<>(new JarInfo(file));
        dl.download();

        return dl;
    }

    @SneakyThrows
    public static SingleJmodDownloader<ClassNode> importJmod(File file) {
        SingleJmodDownloader<ClassNode> dl = new SingleJmodDownloader<>(new JarInfo(file));
        dl.download();

        return dl;
    }

    @SneakyThrows
    public static PhantomJarDownloader<ClassNode> importPhantomJar(File file, Skidfuscator skidfuscator) {
        PhantomJarDownloader<ClassNode> dl = new PhantomJarDownloader<>(
                skidfuscator,
                new SkidASMFactory(skidfuscator),
                new JarInfo(file)
        );
        dl.download();

        return dl;
    }

}