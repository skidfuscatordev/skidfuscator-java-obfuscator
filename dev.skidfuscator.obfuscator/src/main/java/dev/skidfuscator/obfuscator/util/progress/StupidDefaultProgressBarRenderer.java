package dev.skidfuscator.obfuscator.util.progress;

import me.tongfei.progressbar.DefaultProgressBarRenderer;
import me.tongfei.progressbar.ProgressBarStyle;

import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;

public class StupidDefaultProgressBarRenderer extends DefaultProgressBarRenderer {
    public StupidDefaultProgressBarRenderer(ProgressBarStyle style, String unitName, long unitSize, boolean isSpeedShown, DecimalFormat speedFormat, ChronoUnit speedUnit) {
        super(style, unitName, unitSize, isSpeedShown, speedFormat, speedUnit);
    }
}
