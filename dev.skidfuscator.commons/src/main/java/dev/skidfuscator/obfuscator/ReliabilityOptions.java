package dev.skidfuscator.obfuscator;

import lombok.Builder;
import lombok.Getter;
import java.io.File;

/** Explicit controls for reproducible, bounded protection runs. */
@Getter @Builder
public class ReliabilityOptions {
    public enum Profile { CONSERVATIVE, BALANCED, STRONG }
    @Builder.Default private Profile profile = Profile.BALANCED;
    private Long seed;
    private File report;
    private boolean dryRun;
    private boolean summaryReport;
    private boolean failOnDuplicateClasses;
    private boolean offline;
    @Builder.Default private long maxDurationMillis = 300_000;
    @Builder.Default private long maxInputBytes = 512L * 1024 * 1024;
    @Builder.Default private long maxOutputBytes = 512L * 1024 * 1024;
    // Zero uses the profile's growth/instruction budget.
    private double maxGrowthRatio;
    private int maxMethodInstructions;

    public void validate() {
        if (profile == null || maxDurationMillis <= 0 || maxOutputBytes <= 0 || maxInputBytes <= 0
                || !Double.isFinite(maxGrowthRatio) || maxGrowthRatio < 0 || maxMethodInstructions < 0 || maxMethodInstructions > 10000)
            throw new IllegalArgumentException("Invalid protection profile or resource budget");
    }
    public double growthLimit() {
        return maxGrowthRatio > 0 ? maxGrowthRatio : profile == Profile.CONSERVATIVE ? 2 : profile == Profile.BALANCED ? 4 : 8;
    }
    public int instructionLimit() {
        return maxMethodInstructions > 0 ? maxMethodInstructions : profile == Profile.CONSERVATIVE ? 2000 : profile == Profile.BALANCED ? 8000 : 10000;
    }
}
