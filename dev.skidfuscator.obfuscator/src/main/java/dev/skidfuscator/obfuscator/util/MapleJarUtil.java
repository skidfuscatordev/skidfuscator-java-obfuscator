package dev.skidfuscator.obfuscator.util;

import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.reader.DexFileReader;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.SkidASMFactory;
import dev.skidfuscator.obfuscator.creator.SkidLibASMFactory;
import dev.skidfuscator.obfuscator.io.dex.DexJarDownloader;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomJarDownloader;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomResolvingJarDumper;
import lombok.SneakyThrows;
import org.mapleir.asm.ClassNode;
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
                try {
                    boolean exempt = !cn.isVirtual() && skidfuscator.getExemptAnalysis().isExempt(cn);
                    ClassWriter writer = buildClassWriter(
                            skidfuscator.getClassSource().getClassTree(),
                            exempt && classData.getData().length > 0 ? 0 : ClassWriter.COMPUTE_FRAMES);
                    if (exempt && classData.getData().length > 0)
                        new org.objectweb.asm.ClassReader(classData.getData()).accept(new ClassRemapper(writer, skidfuscator.getClassRemapper()),0);
                    else cn.node.accept(new ClassRemapper(writer, skidfuscator.getClassRemapper()));
                    byte[] bytes = writer.toByteArray();
                    String path = skidfuscator.getClassRemapper().mapType(cn.getName()) + ".class";
                    if (skidfuscator.getConfig().getBoolean("fileCrasher.enabled", false)) {
                        path += "/";
                    }
                    JarEntry entry = new JarEntry(path); entry.setTime(0);
                    out.putNextEntry(entry);
                    out.write(bytes);
                    out.closeEntry();
                    return 1;
                } catch (Exception e) {
                    // Never omit a class or silently restore unprotected input bytes.
                    throw new IOException("Failed to emit class " + cn.getName(), e);
                }
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

    @SneakyThrows
    public static DexJarDownloader<ClassNode> importDex(File file, Skidfuscator skidfuscator) {
        DexFileReader reader = new DexFileReader(file);
        DexFileNode node = new DexFileNode();
        reader.accept(node);

        DexJarDownloader<ClassNode> dl = new DexJarDownloader<>(
                skidfuscator,
                new SkidASMFactory(skidfuscator),
                new JarInfo(file)
        );

        dl.download();

        return dl;
    }

}
