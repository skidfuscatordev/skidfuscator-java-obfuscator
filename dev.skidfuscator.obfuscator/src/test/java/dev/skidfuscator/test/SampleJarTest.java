package dev.skidfuscator.test;

import dev.skidfuscator.core.TestSkidfuscator;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.testclasses.jar.SampleApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class SampleJarTest {
    @TempDir Path directory;

    @Test
    void preservesExecutableJarBehaviorAndResources() throws Exception {
        Path input = directory.resolve("input.jar");
        Path output = directory.resolve("output.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, SampleApp.class.getName());
        String className = SampleApp.class.getName().replace('.', '/') + ".class";
        try (JarOutputStream jar = new JarOutputStream(java.nio.file.Files.newOutputStream(input), manifest);
             InputStream bytes = SampleApp.class.getClassLoader().getResourceAsStream(className)) {
            assertNotNull(bytes);
            jar.putNextEntry(new JarEntry(className));
            bytes.transferTo(jar);
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("app.properties"));
            jar.write("mode=protected\n".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        String original = execute(input);
        assertEquals("protected:55", original);
        new Skidfuscator(SkidfuscatorSession.builder()
                .input(input.toFile()).output(output.toFile())
                .runtime(Path.of(System.getProperty("java.home"), "jmods").toFile())
                .config(new java.io.File(TestSkidfuscator.class.getResource("/config/runtime_new.hocon").toURI()))
                .jmod(true).analytics(false).build()).run();
        try (JarFile jar = new JarFile(output.toFile())) {
            assertNotNull(jar.getJarEntry(className));
            assertEquals(SampleApp.class.getName(), jar.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS));
            try (InputStream resource = jar.getInputStream(jar.getJarEntry("app.properties"))) {
                assertEquals("mode=protected\n", new String(resource.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertEquals(original, execute(output));
    }

    private String execute(Path jar) throws Exception {
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xverify:all", "-jar", jar.toString()).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Application did not terminate");
            String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            assertEquals(0, process.exitValue(), result);
            return result;
        } finally {
            process.destroyForcibly();
        }
    }
}
