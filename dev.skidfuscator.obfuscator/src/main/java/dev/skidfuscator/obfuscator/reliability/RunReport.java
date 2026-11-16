package dev.skidfuscator.obfuscator.reliability;

import com.google.gson.GsonBuilder;
import dev.skidfuscator.obfuscator.*;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Machine-readable provenance. Configuration contents and credentials are never copied. */
public final class RunReport {
    private final Map<String,Object> data = new LinkedHashMap<>();
    private final long started = System.nanoTime();
    private final ReliabilityOptions options;
    private final SkidfuscatorSession session;
    private final Path reportTarget;
    private final List<Map<String,Object>> elements = new ArrayList<>();
    private final List<Map<String,Object>> phases = new ArrayList<>();
    public RunReport(SkidfuscatorSession session, long seed) throws IOException {
        this.session = session; this.options = session.getReliability();
        File report = options.getReport();
        if (report == null && session.getOutput() != null) report = new File(session.getOutput()+".report.json");
        reportTarget = report == null ? null : report.toPath().toAbsolutePath().normalize();
        List<File> sources = new ArrayList<>(Arrays.asList(session.getInput(), session.getConfig(),
                session.getRuntime(), session.getExempt(), session.getMappings()));
        if (session.getLibs() != null) sources.addAll(Arrays.asList(session.getLibs()));
        Path output = session.getOutput() == null ? null : session.getOutput().toPath().toAbsolutePath().normalize();
        for (File source : sources) if (source != null) {
            requireDifferent(output, source.toPath(), "Output must differ from every input artifact");
            requireDifferent(reportTarget, source.toPath(), "Report must differ from every input artifact");
        }
        requireDifferent(reportTarget, output, "Report must differ from output");
        if (reportTarget != null) {
            if (Files.isDirectory(reportTarget)) throw new IOException("Report path is a directory");
            Files.createDirectories(reportTarget.getParent());
        }
        data.put("schemaVersion", 1); data.put("toolVersion", Skidfuscator.VERSION);
        var location=Skidfuscator.class.getProtectionDomain().getCodeSource().getLocation();
        try { data.put("toolSha256",hash(new File(location.toURI()))); } catch (java.net.URISyntaxException invalid) { throw new IOException(invalid); }
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("seed", seed); data.put("profile", options.getProfile().name());
        data.put("inputSha256", hash(session.getInput()));
        data.put("configSha256", hash(session.getConfig()));
        List<Map<String,Object>> libraries = new ArrayList<>();
        if (session.getLibs() != null) for (File library : session.getLibs())
            if (library.isFile()) libraries.add(Map.of("name", library.getName(), "sha256", hash(library)));
        libraries.sort(Comparator.comparing(entry -> entry.get("name").toString()));
        data.put("libraries", libraries); data.put("elements", elements); data.put("phases", phases);
        List<Map<String,Object>> runtime = new ArrayList<>();
        File selectedRuntime = session.getRuntime();
        data.put("targetJavaRelease",dev.skidfuscator.jghost.GhostHelper.targetRelease(selectedRuntime));
        if (selectedRuntime.isDirectory()) {
            File[] modules = selectedRuntime.listFiles(f -> f.isFile() && f.getName().endsWith(".jmod"));
            if (modules == null) throw new IOException("Cannot read target runtime: " + selectedRuntime);
            Arrays.sort(modules, Comparator.comparing(File::getName));
            for (File module : modules) runtime.add(Map.of("name",module.getName(),"sha256",hash(module)));
        } else if (selectedRuntime.isFile()) runtime.add(Map.of("name",selectedRuntime.getName(),"sha256",hash(selectedRuntime)));
        data.put("runtime", runtime);
        data.put("limits", Map.of("durationMillis",options.getMaxDurationMillis(), "outputBytes",options.getMaxOutputBytes(),"inputBytes",options.getMaxInputBytes(),
                "growthRatio",options.growthLimit(), "methodInstructions",options.instructionLimit()));
    }
    public void element(String kind, String name, boolean preserve, String reason) {
        elements.add(Map.of("kind",kind,"name",name,"action",preserve?"preserve":"transform","reason",reason));
    }
    public void context(String phase,String element) { data.put("lastPhase",phase);data.put("lastElement",element); }
    public void phase(String name, long nanos) {
        phases.add(Map.of("name",name,"durationMillis",(System.nanoTime()-nanos)/1_000_000));
        checkpoint();
    }
    public void checkpoint() {
        if (Thread.currentThread().isInterrupted() || (System.nanoTime()-started)/1_000_000 > options.getMaxDurationMillis())
            throw new IllegalStateException("Protection time budget exceeded; output was not committed");
    }
    public void outputBudget(Path output) throws IOException {
        checkpoint();
        long size = Files.size(output);
        long inputSize = session.getInput() != null && session.getInput().isFile() ? session.getInput().length() : 0;
        long growth = (long)(inputSize * options.growthLimit()) + 256L * 1024;
        if (size > options.getMaxOutputBytes() || inputSize > 0 && size > growth)
            throw new IOException("Protected JAR exceeds output/growth budget: " + size + " bytes");
        data.put("outputBytes",size);
        data.put("growthRatio",inputSize == 0 ? 0 : (double)size/inputSize);
    }
    public void finish(Throwable failure, List<Transformer> transformers) throws IOException {
        data.put("status",failure != null?"failed":options.isDryRun()?"planned":"succeeded");
        data.put("durationMillis",(System.nanoTime()-started)/1_000_000);
        data.put("heapLimitBytes",Runtime.getRuntime().maxMemory());
        data.put("heapPoolPeakBytes",java.lang.management.ManagementFactory.getMemoryPoolMXBeans().stream().filter(pool->pool.getType()==java.lang.management.MemoryType.HEAP).mapToLong(pool->Math.max(0,pool.getPeakUsage().getUsed())).sum());
        data.put("heapUsedBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        if (failure != null) data.put("error",Map.of("type",failure.getClass().getName(),"message",String.valueOf(failure.getMessage())));
        List<Map<String,Object>> results = new ArrayList<>();
        for (Transformer transformer : transformers) if (transformer instanceof AbstractTransformer) {
            AbstractTransformer t = (AbstractTransformer)transformer;
            results.add(Map.of("name",t.getName(),"success",t.getSuccess(),"skipped",t.getSkipped(),"failed",t.getFailed()));
        }
        data.put("transformers",results);
        data.put("hashPolicy","Java-SE-stable-integer-functions; SSVM Corretto 17");
        if (failure == null && !options.isDryRun()) data.put("outputSha256",hash(session.getOutput()));
        if (reportTarget == null) return; // Embedders without files still use the existing callback API.
        Path target = reportTarget;
        Path temporary = Files.createTempFile(target.getParent(),".report-",".tmp");
        try {
            Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(data));
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    private static void requireDifferent(Path first, Path second, String message) throws IOException {
        if (first != null && second != null && (first.equals(second.toAbsolutePath().normalize())
                || Files.exists(first) && Files.exists(second) && Files.isSameFile(first,second)))
            throw new IOException(message);
    }
    public static String hash(File file) throws IOException {
        if (file == null || !file.isFile()) return "unavailable";
        try (InputStream input = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192]; int count;
            while ((count=input.read(buffer))!=-1) digest.update(buffer,0,count);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
