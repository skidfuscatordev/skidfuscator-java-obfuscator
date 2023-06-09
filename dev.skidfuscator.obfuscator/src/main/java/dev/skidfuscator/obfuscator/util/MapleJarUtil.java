package dev.skidfuscator.obfuscator.util;

import com.esotericsoftware.asm.Type;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.SkidASMFactory;
import dev.skidfuscator.obfuscator.creator.SkidFlowGraphDumper;
import dev.skidfuscator.obfuscator.creator.SkidLibASMFactory;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomJarDownloader;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomResolvingJarDumper;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.verifier.Verifier;
import lombok.SneakyThrows;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.deob.PassGroup;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
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

    public MapleJarUtil() {
    }

    public static void dumpJar(Skidfuscator skidfuscator, PassGroup masterGroup, String outputFile) throws IOException {
        (new PhantomResolvingJarDumper(skidfuscator, skidfuscator.getJarContents(), skidfuscator.getClassSource()) {

            @Override
            public int dumpClass(JarOutputStream out, JarClassData classData) throws IOException {
                ClassNode cn = classData.getClassNode();
                for (org.objectweb.asm.tree.MethodNode method : cn.node.methods) {
                    method.localVariables = null;
                }

                String path = classData.getName();
                if (skidfuscator.getConfig().getBoolean("fileCrasher.enabled", false)) {
                    path += "/";
                }

                JarEntry entry = new JarEntry(path);
                ClassTree tree = skidfuscator.getClassSource().getClassTree();

                //Skidfuscator.LOGGER.post("Writing " + entry.getName());

                if (!cn.isVirtual() && skidfuscator.getExemptAnalysis().isExempt(cn)) {
                    final JarClassData resource = skidfuscator
                            .getJarContents()
                            .getClassContents()
                            .namedMap()
                            .get(classData.getName());

                    if (resource == null) {
                        throw new IllegalStateException("Failed to find class source for " + cn.getName());
                    }
                    out.putNextEntry(entry);

                    ClassWriter writer = this.buildClassWriter(
                            tree,
                            0
                    );
                    ClassRemapper remapper = new ClassRemapper(
                            writer,
                            skidfuscator.getClassRemapper()
                    );
                    cn.node.accept(remapper);
                    out.write(writer.toByteArray());
                    //out.write(resource.getData());
                    return 1;
                }


                for (MethodNode m : cn.getMethods()) {
                    if (m.node.instructions.size() > 10000) {
                        Skidfuscator.LOGGER.warn("large method: " + m + " @" + m.node.instructions.size() + "\n");
                    }
                }

                try {
                    final String name = skidfuscator.getClassRemapper()
                            .mapOrDefault(Type.getObjectType(classData.getName()
                                    .replace(".class", "")
                                    .replace(".", "/")).getInternalName());

                    path = name.replace(".", "/") + ".class";

                    if (skidfuscator.getConfig().getBoolean("fileCrasher.enabled", false)) {
                        path += "/";
                    }

                    entry = new JarEntry(path);
                    out.putNextEntry(entry);
                    //Skidfuscator.LOGGER.post("Wrote " + entry.getName());
                    try {
                        ClassWriter writer = this.buildClassWriter(
                                tree,
                                SkidFlowGraphDumper.TEST_COMPUTE
                                    ? ClassWriter.COMPUTE_MAXS
                                    : ClassWriter.COMPUTE_FRAMES
                        );
                        ClassRemapper remapper = new ClassRemapper(writer, skidfuscator.getClassRemapper());
                        cn.node.accept(remapper);
                        out.write(writer.toByteArray());
                    } catch (Exception var8) {
                        ClassWriter writer = this.buildClassWriter(tree, ClassWriter.COMPUTE_MAXS);
                        cn.node.accept(writer);
                        out.write(writer.toByteArray());
                        var8.printStackTrace();

                        Skidfuscator.LOGGER.error(
                                "\rFailed to write " + cn.getName() + "! Writing with COMPUTE_MAXS, which may cause runtime abnormalities\n",
                                var8
                        );
                    }
                } catch (Exception var9) {
                    Skidfuscator.LOGGER.error(
                            "\rFailed to write " + cn.getName() + "! Skipping class...\n",
                            var9
                    );
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
    public static SingleJarDownloader<ClassNode> importJar(File file, Skidfuscator skidfuscator) {
        SingleJarDownloader<ClassNode> dl = new SingleJarDownloader<>(
                new SkidLibASMFactory(skidfuscator),
                new JarInfo(file)
        );
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