package dev.skidfuscator.obfuscator.transform.impl.sdk;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.SkidApplicationClassSource;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.InitSkidTransformEvent;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomJarDownloader;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.MapleJarUtil;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteio.in.SingleJarDownloader;

import java.io.*;
import java.nio.file.Files;

import static dev.skidfuscator.obfuscator.util.JdkDownloader.CACHE_DIR;

public class SdkInjectorTransformer extends AbstractTransformer {
    public SdkInjectorTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "SDK");
    }

    @Listen
    void handle(final InitSkidTransformEvent event) {
        java.nio.file.Path temporary = null;
        try {
            // Create cache directory if it doesn't exist
            if (!Files.exists(CACHE_DIR)) {
                Files.createDirectories(CACHE_DIR);
            }

            // Extract SDK jar from resources to cache
            temporary = Files.createTempFile(CACHE_DIR, "sdk-", ".jar");
            File sdkFile = temporary.toFile();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("resources/sdk.jar");
                 OutputStream os = new FileOutputStream(sdkFile)) {
                if (is == null) {
                    throw new IOException("Could not find sdk.jar in resources");
                }
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            // Import the SDK jar classes
            final PhantomJarDownloader<ClassNode> downloader = MapleJarUtil.importPhantomJar(
                sdkFile,
                skidfuscator
            );

            // Version the generated runtime namespace so older protected modules can coexist.
            var relocated = new java.util.ArrayList<JarClassData>();
            var factory = new dev.skidfuscator.obfuscator.creator.SkidASMFactory(skidfuscator);
            var remapper = new org.objectweb.asm.commons.Remapper() {
                @Override public String map(String name) {
                    return name.startsWith("sdk/") ? Skidfuscator.SDK_PREFIX + name.substring(4) : name;
                }
            };
            for (JarClassData classData : downloader.getJarContents().getClassContents()) {
                var writer = new org.objectweb.asm.ClassWriter(0);
                new org.objectweb.asm.ClassReader(classData.getData()).accept(new org.objectweb.asm.commons.ClassRemapper(writer,remapper),0);
                byte[] bytes = writer.toByteArray();
                String name = remapper.map(classData.getClassNode().getName());
                if (skidfuscator.getClassSource().isApplicationClass(name))
                    throw new IOException("Input already contains the generated runtime: " + name);
                relocated.add(new JarClassData(name + ".class",bytes,factory.create(bytes,name)));
            }
            downloader.getJarContents().getClassContents().clear();
            downloader.getJarContents().getClassContents().addAll(relocated);
            skidfuscator.getJarContents().getClassContents().addAll(relocated);
            ApplicationClassSource library = new SkidApplicationClassSource("Library",
                    false,
                    downloader.getJarContents(),
                    skidfuscator
            );

            skidfuscator.getClassSource().addLibraries(new LibraryClassSource(library, 5));

        } catch (IOException e) {
            throw new RuntimeException("Failed to inject SDK", e);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); }
            catch (IOException failure) { throw new java.io.UncheckedIOException("Cannot remove SDK temporary file", failure); }
        }
    }
}
