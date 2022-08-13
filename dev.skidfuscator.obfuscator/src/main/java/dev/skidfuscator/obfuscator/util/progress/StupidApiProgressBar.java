package dev.skidfuscator.obfuscator.util.progress;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarConsumer;
import me.tongfei.progressbar.ProgressBarRenderer;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class StupidApiProgressBar extends ProgressBar implements ProgressWrapper {
    public StupidApiProgressBar(String task, long initialMax) {
        super(task, initialMax);
    }

    public StupidApiProgressBar(String task, long initialMax, int updateIntervalMillis, boolean continuousUpdate, PrintStream os, ProgressBarStyle style, String unitName, long unitSize, boolean showSpeed, DecimalFormat speedFormat, ChronoUnit speedUnit, long processed, Duration elapsed) {
        super(task, initialMax, updateIntervalMillis, continuousUpdate, os, style, unitName, unitSize, showSpeed, speedFormat, speedUnit, processed, elapsed);
    }

    public StupidApiProgressBar(String task, long initialMax, int updateIntervalMillis, boolean continuousUpdate, long processed, Duration elapsed, ProgressBarRenderer renderer, ProgressBarConsumer consumer) {
        super(task, initialMax, updateIntervalMillis, continuousUpdate, processed, elapsed, renderer, consumer);
    }

    @Override
    public void tick() {
        super.step();
    }

    @Override
    public void tick(int amount) {
        super.stepBy(amount);
    }
}
