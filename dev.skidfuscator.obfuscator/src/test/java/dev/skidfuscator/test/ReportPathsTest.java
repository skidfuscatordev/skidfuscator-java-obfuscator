package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.*;
import dev.skidfuscator.obfuscator.reliability.RunReport;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReportPathsTest {
    @TempDir Path root;
    @Test void rejectsReplacingInputsThroughOutputOrReportAliases() throws Exception {
        Path input=root.resolve("input.jar"),alias=root.resolve("alias.jar"); Files.writeString(input,"original");
        Files.createLink(alias,input);
        var sameOutput=SkidfuscatorSession.builder().input(input.toFile()).output(alias.toFile()).build();
        assertThrows(IOException.class,()->new RunReport(sameOutput,42));
        var sameReport=SkidfuscatorSession.builder().input(input.toFile()).output(root.resolve("output.jar").toFile())
                .reliability(ReliabilityOptions.builder().report(alias.toFile()).build()).build();
        assertThrows(IOException.class,()->new RunReport(sameReport,42));
        assertEquals("original",Files.readString(input));
    }
    @Test void createsReportDirectoryAndRecordsSelectedRuntimeHash() throws Exception {
        Path runtime=root.resolve("runtime.jar"),report=root.resolve("private/run.json"); Files.writeString(runtime,"runtime");
        var session=SkidfuscatorSession.builder().runtime(runtime.toFile())
                .reliability(ReliabilityOptions.builder().report(report.toFile()).dryRun(true).build()).build();
        new RunReport(session,42).finish(null,Collections.emptyList());
        String contents=Files.readString(report);
        assertTrue(contents.contains(RunReport.hash(runtime.toFile())));
        assertTrue(contents.contains("\"status\": \"planned\""));
    }
}
