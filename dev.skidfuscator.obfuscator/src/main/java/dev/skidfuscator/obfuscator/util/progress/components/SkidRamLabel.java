package dev.skidfuscator.obfuscator.util.progress.components;

import dev.skidfuscator.obfuscator.util.ConsoleColors;
import lukfor.progress.renderer.labels.TimeLabel;
import lukfor.progress.tasks.monitors.TaskMonitor;
import lukfor.progress.util.TimeUtil;

import static org.fusesource.jansi.Ansi.ansi;

public class SkidRamLabel extends TimeLabel {
    @Override
    public void render(TaskMonitor monitor, StringBuilder buffer) {
        if (monitor.isDone())
            return;

        final long freeMemory = Math.round(Runtime.getRuntime().totalMemory() / 1E6);
        final String memory = freeMemory + "";

        final long maxMemory = Math.round(Runtime.getRuntime().maxMemory() / 1E6);
        final String memoryString = (maxMemory == Long.MAX_VALUE
                ? ConsoleColors.GREEN + "no limit"
                : maxMemory + "mb"
        );

        buffer.append(((maxMemory - freeMemory) > 1000) ? ansi().bgGreen() : ansi().bgRed());
        buffer.append("(");
        buffer.append(memory);
        buffer.append(" / ");
        buffer.append(memoryString);
        buffer.append(")");
        buffer.append(ansi().reset());
    }
}
