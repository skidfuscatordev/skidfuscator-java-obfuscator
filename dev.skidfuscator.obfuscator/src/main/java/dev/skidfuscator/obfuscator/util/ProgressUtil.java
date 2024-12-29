package dev.skidfuscator.obfuscator.util;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.util.progress.EmptyProgressBar;
import dev.skidfuscator.obfuscator.util.progress.ProgressWrapper;
import dev.skidfuscator.obfuscator.util.progress.TaskServiceBuilder;
import dev.skidfuscator.obfuscator.util.progress.components.SComponents;
import dev.skidfuscator.obfuscator.util.progress.components.SkidTaskCustom;
import lombok.experimental.UtilityClass;
import lukfor.progress.Components;
import lukfor.progress.renderer.IProgressRenderer;
import lukfor.progress.util.AnsiColors;

import java.util.function.Consumer;

@UtilityClass
public class ProgressUtil {
    public ProgressWrapper progress(final int count, String finalText) {
        return isRunningTest()
                ? new EmptyProgressBar()
                : new TaskServiceBuilder()
                .name("Executing...")//.setInitialMax(count)
                //.setUpdateIntervalMillis(1000)
                //.setStyle(ProgressBarStyle.ASCII)
                //.setSpeedUnit(ChronoUnit.SECONDS)
                //.setUnit("", 1L)
                .style(new SkidTaskCustom(finalText),
                        SComponents.SPINNER,
                        SComponents.SPACE,
                        SComponents.TASK_NAME,
                        SComponents.SPACE,
                        SComponents.PROGRESS_BAR,
                        SComponents.PROGRESS_LABEL,
                        SComponents.SPACE,
                        SComponents.TIME,
                        SComponents.SPACE,
                        SComponents.RAM
                )
                .animated(true)
                .count(count)
                .target(System.out)
                .build();
    }

    public ProgressWrapper progress(final int count) {
        return progress(count, "✨ Done in %%__TIME__%%");
    }

    public ProgressWrapper progressCheck(final int count, String success) {
        return progressCheck(count, success, "");
    }

    public ProgressWrapper progressCheck(final int count, String success, String prefix) {
        return progress(count, prefix + AnsiColors.green("✔") + "  " + success);
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
