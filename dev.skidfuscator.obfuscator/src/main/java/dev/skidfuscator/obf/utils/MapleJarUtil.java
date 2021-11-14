package dev.skidfuscator.obf.utils;

import dev.skidfuscator.obf.phantom.PhantomJarDownloader;
import dev.skidfuscator.obf.phantom.PhantomResolvingJarDumper;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.app.service.CompleteResolvingJarDumper;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.deob.PassGroup;
import org.mapleir.deob.passes.rename.ClassRenamerPass;
import org.objectweb.asm.ClassWriter;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteio.in.AbstractJarDownloader;
import org.topdank.byteio.in.SingleJarDownloader;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * @author Ghast
 * @since 12/12/2020
 * HideMySkewnessCheckObfuscator © 2020
 */
public class MapleJarUtil {
    public static void dumpJar(ApplicationClassSource app, AbstractJarDownloader<ClassNode> dl, PassGroup masterGroup, String outputFile) throws IOException {
        (new PhantomResolvingJarDumper(dl.getJarContents(), app) {
            @Override
            public int dumpResource(JarOutputStream out, String name, byte[] file) throws IOException {
//				if(name.startsWith("META-INF")) {
//					System.out.println(" ignore " + name);
//					return 0;
//				}
                if(name.equals("META-INF/MANIFEST.MF")) {
                    ClassRenamerPass renamer = (ClassRenamerPass) masterGroup.getPass(e -> e.is(ClassRenamerPass.class));

                    if(renamer != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(baos));
                        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(file)));

                        String line;
                        while((line = br.readLine()) != null) {
                            String[] parts = line.split(": ", 2);
                            if(parts.length != 2) {
                                bw.write(line);
                                continue;
                            }

                            if(parts[0].equals("Main-Class")) {
                                String newMain = renamer.getRemappedName(parts[1].replace(".", "/")).replace("/", ".");
                                parts[1] = newMain;
                            }

                            bw.write(parts[0]);
                            bw.write(": ");
                            bw.write(parts[1]);
                            bw.write(System.lineSeparator());
                        }

                        br.close();
                        bw.close();

                        file = baos.toByteArray();
                    }
                }
                return super.dumpResource(out, name, file);
            }

            public int dumpClass(JarOutputStream out, String name, ClassNode cn) throws IOException {
                JarEntry entry = new JarEntry(cn.getName() + ".class");
                out.putNextEntry(entry);
                ClassTree tree = app.getClassTree();

                for (MethodNode m : cn.getMethods()) {
                    if (m.node.instructions.size() > 10000) {
                        System.out.println("large method: " + m + " @" + m.node.instructions.size());
                    }
                }

                try {
                    try {
                        ClassWriter writer = this.buildClassWriter(tree, ClassWriter.COMPUTE_FRAMES);
                        cn.node.accept(writer);
                        out.write(writer.toByteArray());
                    } catch (Exception var8) {
                        ClassWriter writer = this.buildClassWriter(tree, 1);
                        cn.node.accept(writer);
                        out.write(writer.toByteArray());
                        var8.printStackTrace();
                        System.err.println("Failed to write " + cn.getName() + "! Writing with COMPUTE_MAXS, which may cause runtime abnormalities");
                    }
                } catch (Exception var9) {
                    System.err.println("Failed to write " + cn.getName() + "! Skipping class...");
                }

                return 1;
            }
        }).dump(new File(outputFile));
    }

    @SneakyThrows
    public static SingleJarDownloader<ClassNode> importJar(File file) {
        SingleJarDownloader<ClassNode> dl = new SingleJarDownloader<>(new JarInfo(file));
        dl.download();

        return dl;
    }

    @SneakyThrows
    public static PhantomJarDownloader<ClassNode> importPhantomJar(File file) {
        PhantomJarDownloader<ClassNode> dl = new PhantomJarDownloader<>(new JarInfo(file));
        dl.download();

        return dl;
    }

}