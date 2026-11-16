package dev.skidfuscator.obfuscator.util;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.ClassReader;

/** Reads the selected JDK's own module image, including JDKs without jmods. */
public final class RuntimeImage {
    private RuntimeImage() {}

    public static ApplicationClassSource read(File image, Runnable checkpoint) throws IOException {
        Path path = image.toPath().toAbsolutePath().normalize();
        Path home = path.getParent().getParent();
        if (!path.equals(home.resolve("lib/modules")) || !Files.isRegularFile(path)
                || !Files.isRegularFile(home.resolve("lib/jrt-fs.jar")))
            throw new IOException("Select a complete JDK lib/modules image with its lib/jrt-fs.jar");
        Map<String, ClassNode> classes = new LinkedHashMap<>();
        long total = 0;
        try (FileSystem filesystem = FileSystems.newFileSystem(URI.create("jrt:/"), Map.of("java.home", home.toString()));
             var paths = Files.walk(filesystem.getPath("/modules"))) {
            Iterator<Path> entries = paths.filter(p -> p.toString().endsWith(".class")
                    && !p.getFileName().toString().equals("module-info.class")).sorted().iterator();
            while (entries.hasNext()) {
                Path entry = entries.next();
                long size = Files.size(entry);
                total += size;
                if (size > 16L * 1024 * 1024 || total > VerifiedDownloads.MAX_EXPANDED || classes.size() >= 100_000)
                    throw new IOException("Runtime image metadata budget exceeded");
                ClassNode node = ClassHelper.create(Files.readAllBytes(entry),
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                if (classes.putIfAbsent(node.getName(), node) != null)
                    throw new IOException("Duplicate runtime class: " + node.getName());
                if ((classes.size() & 255) == 0) checkpoint.run();
            }
        }
        if (!classes.containsKey("java/lang/Object")) throw new IOException("Runtime image has no java.lang.Object");
        return new ApplicationClassSource(image.getName(), false, classes.values());
    }
}
