package dev.skidfuscator.jghost;

import dev.skidfuscator.jghost.tree.GhostClassNode;
import dev.skidfuscator.jghost.tree.GhostContents;
import dev.skidfuscator.jghost.tree.GhostLibrary;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/** Read declaration metadata one class at a time; never retain library code or resources. */
final class LibraryMetadataReader {
    static GhostLibrary read(File file, int targetRelease) {
        boolean module = file.getName().endsWith(".jmod");
        try (ZipFile archive = module ? new ZipFile(file) : new JarFile(file)) {
            boolean multiRelease = !module && ((JarFile) archive).isMultiRelease();
            Map<String, ZipEntry> selected = new TreeMap<>();
            Map<String, Integer> versions = new HashMap<>();
            Set<String> entries = new HashSet<>();
            Enumeration<? extends ZipEntry> iterator = archive.entries();
            while (iterator.hasMoreElements()) {
                ZipEntry entry = iterator.nextElement(); String name = entry.getName(); int version = 0;
                if (!entries.add(name)) throw new IOException("Duplicate library entry: " + name);
                if (entries.size() > 250_000) throw new IOException("Too many library entries");
                if (module) {
                    if (!name.startsWith("classes/")) continue;
                    name = name.substring(8);
                } else if (name.startsWith("META-INF/versions/")) {
                    if (!multiRelease) continue;
                    String[] parts = name.substring(18).split("/",2);
                    if (parts.length != 2) continue;
                    try { version = Integer.parseInt(parts[0]); } catch (NumberFormatException invalid) { continue; }
                    if (version < 9 || version > targetRelease) continue;
                    name = parts[1];
                }
                if (!name.endsWith(".class") || name.equals("module-info.class")) continue;
                if (version >= versions.getOrDefault(name,-1)) { selected.put(name,entry); versions.put(name,version); }
            }
            GhostLibrary library = new GhostLibrary();
            library.setName(file.getName()); library.setContents(new GhostContents());
            long total = 0;
            for (Map.Entry<String, ZipEntry> entry : selected.entrySet()) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Library import interrupted");
                byte[] bytes;
                try (InputStream input = archive.getInputStream(entry.getValue())) { bytes = input.readNBytes(16 * 1024 * 1024 + 1); }
                total += bytes.length;
                if (bytes.length > 16 * 1024 * 1024 || total > 512L * 1024 * 1024) throw new IOException("Library metadata input budget exceeded");
                ClassNode node = new ClassNode();
                new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                if (!entry.getKey().equals(node.name + ".class")) throw new IOException("Class name does not match library entry: " + entry.getKey());
                library.getContents().getClasses().put(entry.getKey(), GhostClassNode.of(node));
            }
            return library;
        } catch (IOException failure) { throw new UncheckedIOException("Failed to import library: " + file, failure); }
    }
}
