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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
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
        return getLibrary(logger,lib,folder,jre,Runtime.version().feature());
    }

    public GhostLibrary getLibrary(final Logger logger, final File lib, final File folder, boolean jre, int targetRelease) {
        logger.post("[+] " + lib.getAbsolutePath());
        if (!lib.isFile()) throw new IllegalArgumentException("Library does not exist: " + lib);
        final String digest;
        try {
            digest = Files.asByteSource(lib).hash(Hashing.sha256()).toString();
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not hash library " + lib, failure);
        }

        final StringBuilder outputPath = new StringBuilder();
        if (folder != null) {
            outputPath.append(folder.getAbsolutePath()).append("/");
        } else {
            outputPath.append("mappings/");
        }

        if (jre) {
            outputPath.append("jvm/");
        }

        // Version the schema as well as the input bytes. Legacy maps omitted
        // interface/signature metadata and must never satisfy a new lookup.
        outputPath.append("v2/").append(lib.getName()).append("-").append(digest).append("-jdk").append(targetRelease);
        outputPath.append(".json");

        final File output = new File(outputPath.toString());

        final GhostLibrary library;

        if (!output.exists()) {
            logger.post("[?] Could not find mappings for " + lib.getAbsolutePath() + "... Creating...");
            output.getParentFile().mkdirs();
            output.getParentFile().mkdir();
            library = GhostHelper.createFromLibraryFile(logger, lib, targetRelease);
            if (!digest.equals(library.getSha256()))
                throw new IllegalStateException("Library changed while its metadata was being imported: " + lib);
            GhostHelper.saveLibraryFile(logger, library, output);
            logger.post("[✓] Creating mappings for " + lib.getAbsolutePath() + "!");
        } else {
            library = GhostHelper.readFromLibraryFile(logger, output);
            if (!digest.equals(library.getSha256()))
                throw new IllegalStateException("Library metadata digest mismatch: " + output);
        }

        return library;
    }

    public ApplicationClassSource getLibraryClassSource(final SkidfuscatorSession session, final Logger logger, final File lib, boolean jvm) {
        return importFile(logger, session.isFuckIt(), getLibrary(logger, lib, null, jvm, targetRelease(session.getRuntime())));
    }

    public ApplicationClassSource getLibraryClassSource(final Logger logger, final boolean fuckIt, final File lib, boolean jvm) {
        return importFile(logger, fuckIt, getLibrary(logger, lib, jvm));
    }

    public ApplicationClassSource getLibraryClassSource(final SkidfuscatorSession session, final Logger logger, final File lib, final File mappings, final boolean jvm) {
        return importFile(logger, session.isFuckIt(), getLibrary(logger, lib, mappings, jvm, targetRelease(session.getRuntime())));
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
        try (final Reader fileReader = java.nio.file.Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            final GhostLibrary library = Ghost
                    .gson()
                    .fromJson(fileReader, GhostLibrary.class);
            if (library == null || library.getContents() == null)
                throw new IllegalArgumentException("Empty library metadata: " + file);
            return library;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read library metadata: " + file, e);
        }
    }

    public GhostLibrary createFromLibraryFile(final Logger logger, final File file) {
        return createFromLibraryFile(logger,file,Runtime.version().feature());
    }

    public GhostLibrary createFromLibraryFile(final Logger logger, final File file, int targetRelease) {
        final JarInfo jarInfo = new JarInfo(file);
        final AbstractJarDownloader<ClassNode> downloader = file.getName().endsWith(".jmod")
                        ? new SingleJmodDownloader<>(jarInfo)
                        : new SingleJarDownloader<>(jarInfo,targetRelease);

        try {
            downloader.download();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to import library: " + file, e);
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
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to hash library: " + file, e);
        }

        for (JarClassData classContent : downloader.getJarContents().getClassContents()) {
            final GhostClassNode ghostClassNode = GhostClassNode.of(classContent.getClassNode().node);
            ghostContents.getClasses().put(classContent.getName(), ghostClassNode);
        }

        return ghostLibrary;
    }

    public void saveLibraryFile(final Logger logger, final GhostLibrary library, final File file) {
        Path temporary = null;
        try {
            Path target = file.toPath().toAbsolutePath();
            java.nio.file.Files.createDirectories(target.getParent());
            temporary = java.nio.file.Files.createTempFile(target.getParent(), ".mapping-", ".tmp");
            java.nio.file.Files.writeString(temporary, libraryJson(library), StandardCharsets.UTF_8);
            java.nio.file.Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save library metadata: " + file, e);
        } finally {
            if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); }
            catch (IOException cleanup) { logger.error("Could not remove temporary metadata", cleanup); }
        }
    }

    public byte[] serializeLibraryFile(final GhostLibrary library) {
        return libraryJson(library).getBytes(StandardCharsets.UTF_8);
    }

    private String libraryJson(final GhostLibrary library) {
        String json = Ghost.gson().toJson(library, GhostLibrary.class);
        StringBuilder encoded = new StringBuilder(json.length());
        char[] hex = "0123456789abcdef".toCharArray();
        // Class-file strings can contain isolated UTF-16 surrogate code units.
        // JSON escapes retain them exactly without lossy UTF-8 replacement.
        for (int i=0; i<json.length(); i++) {
            char c=json.charAt(i);
            if (Character.isSurrogate(c)) encoded.append("\\u")
                    .append(hex[c>>>12]).append(hex[(c>>>8)&15]).append(hex[(c>>>4)&15]).append(hex[c&15]);
            else encoded.append(c);
        }
        return encoded.toString();
    }

    /** Select multi-release dependency metadata using the deployment JDK, not the tool JVM. */
    public int targetRelease(File runtime) {
        runtime = runtime.getAbsoluteFile();
        if (runtime.getName().equals("rt.jar")) return 8;
        File home = runtime.isDirectory() ? runtime.getParentFile() : runtime.getParentFile().getParentFile();
        File release = new File(home,"release");
        if (!release.isFile()) return Runtime.version().feature();
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(release)) { properties.load(input); }
        catch (IOException failure) { throw new UncheckedIOException("Cannot read selected JDK release",failure); }
        String version = properties.getProperty("JAVA_VERSION","").replace("\"","");
        if (version.startsWith("1.8.")) return 8;
        try { return Integer.parseInt(version.split("[.\\-+]")[0]); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Invalid selected JDK version: " + version,invalid); }
    }
}
