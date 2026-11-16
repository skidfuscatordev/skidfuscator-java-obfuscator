package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.reliability.ArchiveChecks;
import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveChecksTest {
    @TempDir Path root;
    Path archive(String entry, int length) throws IOException {
        Path input=root.resolve("input.jar");
        try (var out=new JarOutputStream(Files.newOutputStream(input))) {
            out.putNextEntry(new JarEntry(entry)); out.write(new byte[length]); out.closeEntry();
        }
        return input;
    }
    @Test void refusesPackagingThatCannotBeTransformedSafely() throws Exception {
        for (String entry:new String[]{"META-INF/versions/17/Probe.class","BOOT-INF/classes/Probe.class",
                "META-INF/SIGNER.SF","META-INF/SIGNER.RSA","META-INF/SIGNER.EC"}) {
            Path input=archive(entry,5);
            assertThrows(IOException.class,()->ArchiveChecks.validate(input.toFile(),100),entry);
        }
    }
    @Test void enforcesExpansionBudgetOnCompressedInput() throws Exception {
        Path input=archive("large-resource",1024);
        assertThrows(IOException.class,()->ArchiveChecks.validate(input.toFile(),100));
    }
    @Test void acceptsOrdinaryResourcesWithinBudget() throws Exception {
        Path input=archive("META-INF/services/example.Service",5);
        assertDoesNotThrow(()->ArchiveChecks.validate(input.toFile(),100));
    }
}
