package dev.skidfuscator.jghost;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import dev.skidfuscator.jghost.tree.GhostClassNode;
import dev.skidfuscator.jghost.tree.GhostContents;
import dev.skidfuscator.jghost.tree.GhostLibrary;
import dev.skidfuscator.logger.Logger;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
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
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@UtilityClass
public class GhostHelper {
    public ApplicationClassSource getLibraryClassSource(final SkidfuscatorSession session, final Logger logger, final File file) {
        return getLibraryClassSource(session, logger, file, false);
    }

    public ApplicationClassSource getJvm(final SkidfuscatorSession session, final Logger logger, final File file) {
        return getLibraryClassSource(session, logger, file, true);
    }

    public ApplicationClassSource getJvm(final Logger logger, final boolean fuckit, final File file) {
        return getLibraryClassSource(logger, fuckit, file, true);
    }

    public ApplicationClassSource getJvm(final Logger logger, final boolean fuckit, final File file, final File mappings) {
        return getLibraryClassSource(logger, fuckit, file, mappings, true);
    }

    public GhostLibrary getLibrary(final Logger logger, final File lib, boolean jre) {
        return getLibrary(logger, lib, null, jre);
    }

    public GhostLibrary getLibrary(final Logger logger, final File lib, final File folder, boolean jre) {
        logger.post("[+] " + lib.getAbsolutePath());

        final StringBuilder outputPath = new StringBuilder();
        if (folder != null) {
            outputPath.append(folder.getAbsolutePath()).append("/");
        } else {
            outputPath.append("mappings/");
        }

        if (jre) {
            outputPath.append("jvm/");
        }

        outputPath.append(lib.getName());
        outputPath.append(".json");

        final File output = new File(outputPath.toString());

        final GhostLibrary library;

        if (!output.exists()) {
            logger.post("[?] Could not find mappings for " + lib.getAbsolutePath() + "... Creating...");
            output.getParentFile().mkdirs();
            output.getParentFile().mkdir();
            library = GhostHelper.createFromLibraryFile(logger, lib);
            GhostHelper.saveLibraryFile(logger, library, output);
            logger.post("[✓] Creating mappings for " + lib.getAbsolutePath() + "!");
        } else {
            library = GhostHelper.readFromLibraryFile(logger, output);
        }

        return library;
    }

    public ApplicationClassSource getLibraryClassSource(final SkidfuscatorSession session, final Logger logger, final File lib, boolean jvm) {
        return importFile(logger, session.isFuckIt(), getLibrary(logger, lib, jvm));
    }

    public ApplicationClassSource getLibraryClassSource(final Logger logger, final boolean fuckIt, final File lib, boolean jvm) {
        return importFile(logger, fuckIt, getLibrary(logger, lib, jvm));
    }

    public ApplicationClassSource getLibraryClassSource(final SkidfuscatorSession session, final Logger logger, final File lib, final File mappings, final boolean jvm) {
        return importFile(logger, session.isFuckIt(), getLibrary(logger, lib, mappings, jvm));
    }

    public ApplicationClassSource getLibraryClassSource(final Logger logger, final boolean fuckit, final File lib, final File mappings, final boolean jvm) {
        return importFile(logger, fuckit, getLibrary(logger, lib, mappings, jvm));
    }

    public ApplicationClassSource importFile(final Logger logger, final boolean fuckit, final GhostLibrary library) {
        /* Create a new library class source with superior to default priority */
        final ApplicationClassSource libraryClassSource = new ApplicationClassSource(
                library.getName(),
                fuckit,
                library.getContents()
                        .getClasses()
                        .values()
                        .stream()
                        .map(e -> ClassHelper.create(e.read())).collect(Collectors.toList())
        );
        logger.post("[✓] Imported " + library.getContents().getClasses().size() + " library classes...");

        return libraryClassSource;
    }

    public GhostLibrary readFromLibraryFile(final Logger logger, final File file) {
        try {
            final FileReader fileReader = new FileReader(file);
            final GhostLibrary library = Ghost
                    .gson()
                    .fromJson(fileReader, GhostLibrary.class);
            fileReader.close();
            return library;
        } catch (IOException e) {
            logger.error("Failed to download library cache", e);
            return null;
        }
    }

    public GhostLibrary createFromLibraryFile(final Logger logger, final File file) {
        final JarInfo jarInfo = new JarInfo(file);
        final AbstractJarDownloader<ClassNode> downloader = file.getName().endsWith(".jmod")
                        ? new SingleJmodDownloader<>(jarInfo)
                        : new SingleJarDownloader<>(jarInfo);

        try {
            downloader.download();
        } catch (IOException e) {
            logger.error("Failed to download library", e);
            return null;
        }

        final GhostContents ghostContents = new GhostContents();
        final GhostLibrary ghostLibrary = new GhostLibrary();
        ghostLibrary.setName(file.getName());
        ghostLibrary.setContents(ghostContents);

        try {
            final ByteSource byteSource = Files.asByteSource(file);
            ghostLibrary.setMd5(byteSource.hash(Hashing.md5()).toString());
            ghostLibrary.setSha1(byteSource.hash(Hashing.sha1()).toString());
            ghostLibrary.setSha256(byteSource.hash(Hashing.sha256()).toString());
        } catch (Throwable e) {
            logger.error("Failed to hash library", e);
            return null;
        }

        for (JarClassData classContent : downloader.getJarContents().getClassContents()) {
            final GhostClassNode ghostClassNode = GhostClassNode.of(classContent.getClassNode().node);
            ghostContents.getClasses().put(classContent.getName(), ghostClassNode);
        }

        return ghostLibrary;
    }

    public void saveLibraryFile(final Logger logger, final GhostLibrary library, final File file) {
        try {
            final FileWriter fileWriter = new FileWriter(file);
            final BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.write(Ghost.gson().toJson(library, GhostLibrary.class));
            writer.close();
        } catch (IOException e) {
            logger.error("Failed to download library cache", e);
            return;
        }
    }

    public byte[] serializeLibraryFile(final GhostLibrary library) {
        return Ghost.gson().toJson(library, GhostLibrary.class).getBytes(StandardCharsets.UTF_8);
    }
}
