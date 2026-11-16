package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.util.RuntimeImage;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeImageTest {
    @TempDir Path root;
    @Test void importsOwnImageWithoutLoadingTargetApplicationCode() throws Exception {
        var source=RuntimeImage.read(Path.of(System.getProperty("java.home"),"lib/modules").toFile(),()->{});
        assertNotNull(source.findClassNode("java/lang/Object"));
        assertNotNull(source.findClassNode("java/io/Serializable"));
        var list=source.findClassNode("java/util/ArrayList");
        assertTrue(list.getInterfaces().contains("java/io/Serializable"));
        assertTrue(list.getMethods().stream().allMatch(m->m.node.instructions.size()==0));
    }
    @Test void rejectsIncompleteRuntimeImage() throws Exception {
        Files.createDirectories(root.resolve("lib")); Path image=root.resolve("lib/modules"); Files.writeString(image,"incomplete");
        assertThrows(IOException.class,()->RuntimeImage.read(image.toFile(),()->{}));
    }
}
