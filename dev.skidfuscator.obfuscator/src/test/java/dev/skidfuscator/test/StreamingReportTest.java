package dev.skidfuscator.test;

import com.google.gson.*;
import dev.skidfuscator.obfuscator.*;
import dev.skidfuscator.obfuscator.reliability.RunReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StreamingReportTest {
    @TempDir Path directory;
    RunReport report(Path destination,boolean summary) throws Exception {
        Path runtime = directory.resolve("rt.jar"); Files.writeString(runtime,"fixture");
        return new RunReport(SkidfuscatorSession.builder().runtime(runtime.toFile())
                .reliability(ReliabilityOptions.builder().dryRun(true).summaryReport(summary).report(destination.toFile()).build()).build(),42);
    }
    @Test void fullAndSummaryReportsRetainCountsAndCompleteDiagnostics() throws Exception {
        for (boolean summary : new boolean[]{false,true}) {
            Path path = directory.resolve(summary+".json"); var report = report(path,summary);
            try (var phase = report.stage("test")) {
                assertEquals("running",JsonParser.parseString(Files.readString(path)).getAsJsonObject().get("status").getAsString());
                for (int i=0;i<20000;i++) report.element("method","Test#method"+i,false,"selected");
                report.beforeListener("Example"); report.afterListener("Example",1234);
            }
            report.finish(null,Collections.emptyList());
            var result = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            assertEquals("planned",result.get("status").getAsString());
            assertEquals(20000,result.getAsJsonObject("elementCounts").get("method/transform").getAsInt());
            assertEquals(summary ? 0 : 20000,result.getAsJsonArray("elements").size());
            assertTrue(result.getAsJsonArray("phases").get(0).getAsJsonObject().has("allocatedBytes"));
            assertEquals("Example",result.get("lastTransformer").getAsString());
        }
        try (var files = Files.list(directory)) { assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".report-"))); }
    }
    @Test void failedReportRetainsTheFaultingTransformerAndDecisions() throws Exception {
        Path path = directory.resolve("failed.json"); var report = report(path,false);
        report.element("class","Example",true,"exclusion"); report.beforeListener("Broken pass");
        report.finish(new IllegalStateException("example failure"),Collections.emptyList());
        var result = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals("failed",result.get("status").getAsString());
        assertEquals("Broken pass",result.get("lastTransformer").getAsString());
        assertEquals(1,result.getAsJsonArray("elements").size());
    }
}
