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
        try {
            // Create cache directory if it doesn't exist
            if (!Files.exists(CACHE_DIR)) {
                Files.createDirectories(CACHE_DIR);
            }

            // Extract SDK jar from resources to cache
            File sdkFile = CACHE_DIR.resolve("sdk.jar").toFile();
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

            // Add SDK classes to the input jar
            for (JarClassData classData : downloader.getJarContents().getClassContents()) {
                skidfuscator.getJarContents().getClassContents().add(classData);
            }
            ApplicationClassSource library = new SkidApplicationClassSource("Library",
                    false,
                    downloader.getJarContents(),
                    skidfuscator
            );

            skidfuscator.getClassSource().addLibraries(new LibraryClassSource(library, 5));

        } catch (IOException e) {
            throw new RuntimeException("Failed to inject SDK", e);
        }
    }
}
