package dev.skidfuscator.obfuscator;

import lombok.Builder;

import java.io.File;

/**
 * The Skidfuscator session object to be able to configure a session
 * with the obfuscator.
 */
@Builder
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


    /**
     *
     * @return the input
     */
    public File getInput() {
        return input;
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

    public boolean isRenamer() {
        return renamer;
    }

    public boolean isNative() {
        return c2j;
    }

    public boolean isLowCon() {
        return lowCon;
    }

    public void setLowCon(boolean lowCon) {
        this.lowCon = lowCon;
    }
}
