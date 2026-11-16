package dev.skidfuscator.obfuscator.reliability;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/** Admission checks run before importing entries into the IR. */
public final class ArchiveChecks {
    private ArchiveChecks() {}
    public static void validate(File input,long maxExpanded) throws IOException {
        if (input==null) return; // In-memory embedding API.
        if (!input.isFile()) throw new IOException("Input JAR does not exist: "+input);
        try (JarFile jar=new JarFile(input)) {
            if (jar.size()>100_000) throw new IOException("Too many input entries");
            long total=0; Set<String> names=new HashSet<>(); byte[] buffer=new byte[8192];
            var entries=jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry=entries.nextElement(); String name=entry.getName();
                if (!names.add(name)) throw new IOException("Duplicate input entry: "+name);
                if (name.startsWith("BOOT-INF/")) throw new IOException("Protect the thin application JAR before Spring Boot repackaging");
                if (name.startsWith("META-INF/versions/")) throw new IOException("Multi-release inputs require per-release protection; refusing to flatten versioned classes");
                if (name.matches("(?i)META-INF/[^/]+\\.(SF|RSA|DSA|EC)"))
                    throw new IOException("Protect the unsigned build and sign the final JAR; stale input signatures cannot be retained");
                if (entry.getSize()>maxExpanded) throw new IOException("Input expansion budget exceeded");
                try (InputStream stream=jar.getInputStream(entry)) {
                    int count; while ((count=stream.read(buffer))!=-1) {
                        total+=count;
                        if (total>maxExpanded) throw new IOException("Input expansion budget exceeded");
                    }
                }
            }
        }
    }
}
