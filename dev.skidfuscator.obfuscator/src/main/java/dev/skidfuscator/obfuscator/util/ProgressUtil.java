package dev.skidfuscator.obfuscator.util;

import lombok.experimental.UtilityClass;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class ProgressUtil {
    public ProgressBar progress(final int count) {
        return new ProgressBarBuilder()
                .setTaskName("Executing...").setInitialMax(count)
                .setUpdateIntervalMillis(1000)
                .setStyle(ProgressBarStyle.ASCII)
                .setSpeedUnit(ChronoUnit.SECONDS)
                .setUnit("", 1L)
                .build();
    }
}
