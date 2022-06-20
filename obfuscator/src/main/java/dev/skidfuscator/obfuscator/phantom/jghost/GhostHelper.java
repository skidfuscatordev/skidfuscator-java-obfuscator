package dev.skidfuscator.obfuscator.phantom.jghost;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.phantom.jghost.tree.GhostClassNode;
import dev.skidfuscator.obfuscator.phantom.jghost.tree.GhostContents;
import dev.skidfuscator.obfuscator.phantom.jghost.tree.GhostLibrary;
import lombok.experimental.UtilityClass;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteio.in.AbstractJarDownloader;
import org.topdank.byteio.in.SingleJarDownloader;
import org.topdank.byteio.in.SingleJmodDownloader;

import java.io.*;
import java.util.stream.Collectors;

import static dev.skidfuscator.obfuscator.Skidfuscator.LOGGER;

@UtilityClass
public class GhostHelper {
    public ApplicationClassSource getLibrary(final SkidfuscatorSession session, final File file) {
        return getLibrary(session, file, false);
    }

    public ApplicationClassSource getJvm(final SkidfuscatorSession session, final File file) {
        return getLibrary(session, file, true);
    }

    private ApplicationClassSource getLibrary(final SkidfuscatorSession session, final File lib, boolean jvm) {
        LOGGER.post("[+] " + lib.getAbsolutePath());

        final StringBuilder outputPath = new StringBuilder("mappings/");
        if (jvm) {
            outputPath.append("jvm/");
        }

        outputPath.append(lib.getName());
        outputPath.append(".json");

        final File output = new File(outputPath.toString());
        final GhostLibrary library;

        if (!output.exists()) {
            LOGGER.post("[?] Could not find mappings for " + lib.getAbsolutePath() + "... Creating...");
            output.getParentFile().mkdirs();
            library = GhostHelper.createFromLibraryFile(lib);
            GhostHelper.saveLibraryFile(library, output);
            LOGGER.post("[✓] Creating mappings for " + lib.getAbsolutePath() + "!");
        } else {
            library = GhostHelper.readFromLibraryFile(output);
        }

        /* Create a new library class source with superior to default priority */
        final ApplicationClassSource libraryClassSource = new ApplicationClassSource(
                "libraries",
                session.isFuckIt(),
                library.getContents()
                        .getClasses()
                        .values()
                        .stream()
                        .map(e -> ClassHelper.create(e.read())).collect(Collectors.toList())
        );
        LOGGER.post("[✓] Imported " + library.getContents().getClasses().size() + " library classes...");


        return libraryClassSource;
    }

    public GhostLibrary readFromLibraryFile(final File file) {
        try {
            final FileReader fileReader = new FileReader(file);
            final GhostLibrary library = Ghost
                    .gson()
                    .fromJson(fileReader, GhostLibrary.class);
            fileReader.close();
            return library;
        } catch (IOException e) {
            LOGGER.error("Failed to download library cache", e);
            return null;
        }
    }

    public GhostLibrary createFromLibraryFile(final File file) {
        final JarInfo jarInfo = new JarInfo(file);
        final AbstractJarDownloader<ClassNode> downloader = file.getName().endsWith(".jmod")
                        ? new SingleJmodDownloader<>(jarInfo)
                        : new SingleJarDownloader<>(jarInfo);

        try {
            downloader.download();
        } catch (IOException e) {
            LOGGER.error("Failed to download library", e);
            return null;
        }

        final GhostContents ghostContents = new GhostContents();
        final GhostLibrary ghostLibrary = new GhostLibrary();
        ghostLibrary.setContents(ghostContents);

        try {
            final ByteSource byteSource = Files.asByteSource(file);
            ghostLibrary.setMd5(byteSource.hash(Hashing.md5()).toString());
            ghostLibrary.setSha1(byteSource.hash(Hashing.sha1()).toString());
            ghostLibrary.setSha256(byteSource.hash(Hashing.sha256()).toString());
        } catch (Throwable e) {
            LOGGER.error("Failed to hash library", e);
            return null;
        }

        for (JarClassData classContent : downloader.getJarContents().getClassContents()) {
            final GhostClassNode ghostClassNode = GhostClassNode.of(classContent.getClassNode().node);
            ghostContents.getClasses().put(classContent.getName(), ghostClassNode);
        }

        return ghostLibrary;
    }

    public void saveLibraryFile(final GhostLibrary library, final File file) {
        try {
            final FileWriter fileWriter = new FileWriter(file);
            final BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.write(Ghost.gson().toJson(library, GhostLibrary.class));
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Failed to download library cache", e);
            return;
        }
    }
}
