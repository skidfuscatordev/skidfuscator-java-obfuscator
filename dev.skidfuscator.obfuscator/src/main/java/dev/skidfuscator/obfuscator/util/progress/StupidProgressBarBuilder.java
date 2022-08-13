package dev.skidfuscator.obfuscator.util.progress;

import me.tongfei.progressbar.ProgressBarConsumer;
import me.tongfei.progressbar.ProgressBarStyle;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author Tongfei Chen
 * @since 0.7.0
 */
public class StupidProgressBarBuilder {

    private String task = "";
    private long initialMax = -1;
    private int updateIntervalMillis = 1000;
    private boolean continuousUpdate = false;
    private ProgressBarStyle style = ProgressBarStyle.COLORFUL_UNICODE_BLOCK;
    private ProgressBarConsumer consumer = null;
    private String unitName = "";
    private long unitSize = 1;
    private boolean showSpeed = false;
    private DecimalFormat speedFormat;
    private ChronoUnit speedUnit = ChronoUnit.SECONDS;
    private long processed = 0;
    private Duration elapsed = Duration.ZERO;
    private int maxRenderedLength = -1;

    public StupidProgressBarBuilder() { }

    public StupidProgressBarBuilder setTaskName(String task) {
        this.task = task;
        return this;
    }

    boolean initialMaxIsSet() {
        return this.initialMax != -1;
    }

    public StupidProgressBarBuilder setInitialMax(long initialMax) {
        this.initialMax = initialMax;
        return this;
    }

    public StupidProgressBarBuilder setStyle(ProgressBarStyle style) {
        this.style = style;
        return this;
    }

    public StupidProgressBarBuilder setUpdateIntervalMillis(int updateIntervalMillis) {
        this.updateIntervalMillis = updateIntervalMillis;
        return this;
    }

    public StupidProgressBarBuilder continuousUpdate() {
        this.continuousUpdate = true;
        return this;
    }

    public StupidProgressBarBuilder setConsumer(ProgressBarConsumer consumer) {
        this.consumer = consumer;
        return this;
    }

    public StupidProgressBarBuilder setUnit(String unitName, long unitSize) {
        this.unitName = unitName;
        this.unitSize = unitSize;
        return this;
    }

    public StupidProgressBarBuilder setMaxRenderedLength(int maxRenderedLength) {
        this.maxRenderedLength = maxRenderedLength;
        return this;
    }

    public StupidProgressBarBuilder showSpeed() {
        return showSpeed(new DecimalFormat("#.0"));
    }

    public StupidProgressBarBuilder showSpeed(DecimalFormat speedFormat) {
        this.showSpeed = true;
        this.speedFormat = speedFormat;
        return this;
    }

    public StupidProgressBarBuilder setSpeedUnit(ChronoUnit speedUnit) {
        this.speedUnit = speedUnit;
        return this;
    }

    /**
     * Sets elapsedBeforeStart duration and number of processed units.
     * @param processed amount of processed units
     * @param elapsed duration of
     */
    public StupidProgressBarBuilder startsFrom(long processed, Duration elapsed) {
        this.processed = processed;
        this.elapsed = elapsed;
        return this;
    }

    public StupidApiProgressBar build() {
        return new StupidApiProgressBar(
                task,
                initialMax,
                updateIntervalMillis,
                continuousUpdate,
                processed,
                elapsed,
                new StupidDefaultProgressBarRenderer(style, unitName, unitSize, showSpeed, speedFormat, speedUnit),
                consumer == null ? Util.createConsoleConsumer(maxRenderedLength) : consumer
        );
    }
}
