package dev.skidfuscator.obfuscator.util.misc;

import dev.skidfuscator.obfuscator.util.IOUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Files {
    /**
     * Writes a map to an archive.
     *
     * @param compress
     * 		Enable zip compression.
     * @param output
     * 		File location of jar.
     * @param content
     * 		Contents to write to location.
     *
     * @throws IOException
     * 		When the jar file cannot be written to.
     */
    public static void writeArchive(boolean compress, File output, Map<String, byte[]> content) throws IOException {
        String extension = IOUtil.getExtension(output.toPath());
        // Use buffered streams, reduce overall file write operations
        OutputStream os = new BufferedOutputStream(java.nio.file.Files.newOutputStream(output.toPath()), 1048576);
        try (ZipOutputStream jos = ("zip".equals(extension)) ? new ZipOutputStream(os) :
                /* Let's assume it's a jar */ new JarOutputStream(os)) {
            Set<String> dirsVisited = new HashSet<>();
            // Contents is iterated in sorted order (because 'archiveContent' is TreeMap).
            // This allows us to insert directory entries before file entries of that directory occur.
            CRC32 crc = new CRC32();
            for (Map.Entry<String, byte[]> entry : content.entrySet()) {
                String key = entry.getKey();
                byte[] out = entry.getValue();
                // Write directories for upcoming entries if necessary
                // - Ugly, but does the job.
                if (key.contains("/")) {
                    // Record directories
                    String parent = key;
                    List<String> toAdd = new ArrayList<>();
                    do {
                        parent = parent.substring(0, parent.lastIndexOf('/'));
                        if (dirsVisited.add(parent)) {
                            toAdd.add(0, parent + '/');
                        } else break;
                    } while (parent.contains("/"));
                    // Put directories in order of depth
                    for (String dir : toAdd) {
                        jos.putNextEntry(new JarEntry(dir));
                        jos.closeEntry();
                    }
                }
                // Write entry content
                crc.reset();
                crc.update(out, 0, out.length);
                JarEntry outEntry = new JarEntry(key);
                outEntry.setMethod(compress ? ZipEntry.DEFLATED : ZipEntry.STORED);
                if (!compress) {
                    outEntry.setSize(out.length);
                    outEntry.setCompressedSize(out.length);
                }
                outEntry.setCrc(crc.getValue());
                jos.putNextEntry(outEntry);
                jos.write(out);
                jos.closeEntry();
            }
        }
    }

    public static void purgeDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory())
                purgeDirectory(file);
            file.delete();
        }
    }
}