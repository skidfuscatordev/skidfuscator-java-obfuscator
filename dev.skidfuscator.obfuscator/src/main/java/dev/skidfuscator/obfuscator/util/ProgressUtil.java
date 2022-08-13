package dev.skidfuscator.obfuscator.util;

import dev.skidfuscator.obfuscator.util.progress.EmptyProgressBar;
import dev.skidfuscator.obfuscator.util.progress.ProgressWrapper;
import dev.skidfuscator.obfuscator.util.progress.StupidProgressBarBuilder;
import lombok.experimental.UtilityClass;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class ProgressUtil {
    public ProgressWrapper progress(final int count) {
        return isRunningTest()
                ? new EmptyProgressBar()
                : new StupidProgressBarBuilder()
                .setTaskName("Executing...").setInitialMax(count)
                .setUpdateIntervalMillis(1000)
                .setStyle(ProgressBarStyle.ASCII)
                .setSpeedUnit(ChronoUnit.SECONDS)
                .setUnit("", 1L)
                .build();
    }

    private Boolean isRunningTest = null;

    private boolean isRunningTest() {
        if (isRunningTest == null) {
            isRunningTest = true;
            try {
                Class.forName("org.junit.jupiter.api.Test");
            } catch (ClassNotFoundException e) {
                isRunningTest = false;
            }
        }
        return isRunningTest;
    }

}
