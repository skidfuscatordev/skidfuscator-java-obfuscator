package dev.skidfuscator.obfuscator.util.progress.components;

import lukfor.progress.renderer.labels.TimeLabel;
import lukfor.progress.tasks.monitors.TaskMonitor;

public class SkidTimeLabel extends TimeLabel {
    @Override
    public void render(TaskMonitor monitor, StringBuilder buffer) {
        if (monitor.isDone())
            return;

        super.render(monitor, buffer);
    }
}
