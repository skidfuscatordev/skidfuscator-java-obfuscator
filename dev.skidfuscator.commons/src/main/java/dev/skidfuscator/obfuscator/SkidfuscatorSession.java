package dev.skidfuscator.obfuscator;

import dev.skidfuscator.jvm.Jvm;
import lombok.Builder;

import java.io.File;

/**
 * The Skidfuscator session object to be able to configure a session
 * with the obfuscator.
 */
@Builder
@lombok.AllArgsConstructor
public class SkidfuscatorSession {
    private File input;
    private File output;
    private File[] libs;
    private File mappings;
    private File exempt;
    private File config;
    private File runtime;
    private boolean phantom;
    private boolean jmod;
    private boolean fuckit;
    private boolean analytics;
    private boolean renamer;
    private boolean c2j;

    private boolean lowCon;
    private boolean dex;
    private boolean debug = false;
    private ReliabilityOptions reliability;

    public ReliabilityOptions getReliability() {
        return reliability == null ? ReliabilityOptions.builder().build() : reliability;
    }

    /** Preserve the existing embedding API. */
    public SkidfuscatorSession(File input, File output, File[] libs, File mappings, File exempt,
            File config, File runtime, boolean phantom, boolean jmod, boolean fuckit,
            boolean analytics, boolean renamer, boolean c2j, boolean lowCon, boolean dex, boolean debug) {
        this(input, output, libs, mappings, exempt, config, runtime, phantom, jmod, fuckit,
                analytics, renamer, c2j, lowCon, dex, debug, null);
    }

    /**
     *
     * @return the input
     */
    public File getInput() {
        return input;
    }

    public void setInput(File input) {
        this.input = input;
    }

    /**
     * @return the output
     */
    public File getOutput() {
        return output;
    }

    /**
     * @return the libs
     */
    public File[] getLibs() {
        return libs;
    }

    /**
     * @return the mappings file
     */
    public File getMappings() {
        return mappings;
    }

    /**
     * @return the config file
     */
    public File getConfig() {
        return config;
    }

    /**
     * @return the exempt
     */
    public File getExempt() {
        return exempt;
    }

    /**
     * @return the runtime
     */
    public File getRuntime() {
        if (runtime == null) {
            final String home = System.getProperty("java.home");
            File selected = new File(
                    home,
                    Jvm.getJavaVersion() > 8
                            ? "jmods"
                            : "lib/rt.jar"
            );
            return selected.exists() || Jvm.getJavaVersion() <= 8 ? selected : new File(home,"lib/modules");
        }
        return runtime;
    }

    /**
     * @return the boolean whether the execution uses JPhantom
     */
    public boolean isPhantom() {
        return phantom;
    }

    /**
     * @return the boolean whether the runtime lib is in JMod format
     */
    public boolean isJmod() {
        if (runtime == null)
            return Jvm.isJmod();

        return jmod;
    }

    /**
     * @return  the bool of whether the person is mentally ill and
     *          is willing to skip the forced phantom generation
     */
    public boolean isFuckIt() {
        return fuckit;
    }

    public boolean isAnalytics() {
        return analytics;
    }

    public boolean isDex() {
        return dex;
    }

    public boolean isDebug() {
        return debug;
    }
}
