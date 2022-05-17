package dev.skidfuscator.obfuscator.util;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.SkidASMFactory;
import dev.skidfuscator.obfuscator.phantom.PhantomJarDownloader;
import dev.skidfuscator.obfuscator.phantom.PhantomResolvingJarDumper;
import lombok.SneakyThrows;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.deob.PassGroup;
import org.mapleir.deob.passes.rename.ClassRenamerPass;
import org.objectweb.asm.ClassWriter;
import org.topdank.byteengineer.commons.asm.DefaultASMFactory;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteio.in.AbstractJarDownloader;
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

                if (skidfuscator.getExemptAnalysis().isExempt(cn)) {
                    out.write(
                            skidfuscator
                            .getJarDownloader()
                            .getJarContents()
                            .getClassData()
                            .namedMap().get(cn.getName() + ".class")
                            .getData()
                    );
                    return 1;
                }

                ClassTree tree = skidfuscator.getClassSource().getClassTree();

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
                        ClassWriter writer = this.buildClassWriter(tree, ClassWriter.COMPUTE_MAXS);
                        cn.node.accept(writer);
                        out.write(writer.toByteArray());
                        var8.printStackTrace();
                        System.err.println("Failed to write " + cn.getName() + "! Writing with COMPUTE_MAXS, which may cause runtime abnormalities");
                    }
                } catch (Exception var9) {
                    System.err.println("Failed to write " + cn.getName() + "! Skipping class...");
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