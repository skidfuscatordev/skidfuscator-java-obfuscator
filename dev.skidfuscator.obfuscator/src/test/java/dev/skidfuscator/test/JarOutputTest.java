package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomResolvingJarDumper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarContents;
import org.topdank.byteengineer.commons.data.JarResource;
import java.io.IOException;
import java.nio.file.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class JarOutputTest {
    @TempDir Path directory;

    private byte[] classBytes(boolean valid) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "Sample", null, "java/lang/Object", null);
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "value", "()I", null, null);
        method.visitCode();
        if (valid) method.visitInsn(Opcodes.ICONST_1);
        method.visitInsn(valid ? Opcodes.IRETURN : Opcodes.RETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private PhantomResolvingJarDumper rawDumper(byte[] bytes) {
        JarContents contents = new JarContents();
        contents.getClassContents().add(new JarClassData("Sample.class", bytes, null));
        contents.getResourceContents().add(new JarResource("app.properties", new byte[]{1, 2, 3}));
        return new PhantomResolvingJarDumper(null, contents, null) {
            @Override public int dumpClass(JarOutputStream out, JarClassData data) throws IOException {
                out.putNextEntry(new JarEntry(data.getName()));
                out.write(data.getData());
                out.closeEntry();
                return 1;
            }
        };
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test void replacesDestinationOnlyWithACompleteValidatedJar() throws Exception {
        Path target = directory.resolve("app.jar");
        Files.writeString(target, "previous artifact");
        rawDumper(classBytes(true)).dump(target.toFile());
        try (JarFile jar = new JarFile(target.toFile())) {
            assertEquals(2, jar.size());
            assertNotNull(jar.getEntry("Sample.class"));
            assertNotNull(jar.getEntry("app.properties"));
        }
        assertNoTemporaryFiles();
    }

    @Test void invalidBytecodePreservesDestinationAndRemovesTemporaryOutput() throws Exception {
        Path target = directory.resolve("app.jar");
        Files.writeString(target, "previous artifact");
        IOException failure = assertThrows(IOException.class, () -> rawDumper(classBytes(false)).dump(target.toFile()));
        assertTrue(failure.getMessage().contains("Invalid output class"));
        assertEquals("previous artifact", Files.readString(target));
        assertNoTemporaryFiles();
    }

    @Test void failedWriteDoesNotLeaveNewDestinationOrTemporaryOutput() throws Exception {
        JarContents contents = new JarContents();
        contents.getResourceContents().add(new JarResource("app.properties", new byte[0]));
        var dumper = new PhantomResolvingJarDumper(null, contents, null) {
            @Override public int dumpResource(JarOutputStream out, String name, byte[] data) throws IOException {
                throw new IOException("simulated disk failure");
            }
        };
        Path target = directory.resolve("new.jar");
        assertThrows(IOException.class, () -> dumper.dump(target.toFile()));
        assertFalse(Files.exists(target));
        assertNoTemporaryFiles();
    }

    @Test void duplicateEntriesPreserveDestination() throws Exception {
        JarContents contents = new JarContents();
        contents.getResourceContents().add(new JarResource("same", new byte[]{1}));
        contents.getResourceContents().add(new JarResource("same", new byte[]{2}));
        Path target = directory.resolve("app.jar");
        Files.writeString(target, "previous artifact");
        assertThrows(IOException.class, () -> new PhantomResolvingJarDumper(null, contents, null).dump(target.toFile()));
        assertEquals("previous artifact", Files.readString(target));
        assertNoTemporaryFiles();
    }
}
