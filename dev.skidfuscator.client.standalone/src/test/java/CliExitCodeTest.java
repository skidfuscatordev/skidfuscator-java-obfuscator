import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class CliExitCodeTest {
    @TempDir Path directory;

    private int execute(String... arguments) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("dev.skidfuscator.obfuscator.SkidfuscatorMain");
        command.addAll(java.util.Arrays.asList(arguments));
        Path log = directory.resolve("cli.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "CLI did not terminate");
            String output = java.nio.file.Files.readString(log);
            assertFalse(output.contains("ClassNotFoundException"), output);
            return process.exitValue();
        } finally { process.destroyForcibly(); }
    }

    @Test void helpExitsSuccessfully() throws Exception {
        assertEquals(0, execute("obfuscate", "--help"));
    }

    @Test void invalidArgumentsExitWithUsageError() throws Exception {
        assertEquals(2, execute("obfuscate", "--invalid-option"));
    }

    @Test void missingInputExitsWithExecutionError() throws Exception {
        assertEquals(1, execute("obfuscate", directory.resolve("missing.jar").toString(), "--notrack"));
    }
}
