package dev.skidfuscator.obfuscator.util;

import lombok.experimental.UtilityClass;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class ProgressUtil {
    public ProgressBar progress(final int count) {
        return new ProgressBar("Executing...",
                count,
                1000,
                System.err,
                ProgressBarStyle.ASCII,
                "",
                1L,
                false,
                (DecimalFormat)null,
                ChronoUnit.SECONDS,
                0L,
                Duration.ZERO
        );
    }
}
