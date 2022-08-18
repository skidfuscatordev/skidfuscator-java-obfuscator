package dev.skidfuscator.obfuscator.util.progress.components;

import lukfor.progress.renderer.bars.DefaultProgressBar;
import lukfor.progress.tasks.monitors.TaskMonitor;

public class SkidDefaultProgressBar extends DefaultProgressBar {
    @Override
    public void render(TaskMonitor monitor, StringBuilder buffer) {
        if (monitor.isDone())
            return;

        super.render(monitor, buffer);
    }
}
