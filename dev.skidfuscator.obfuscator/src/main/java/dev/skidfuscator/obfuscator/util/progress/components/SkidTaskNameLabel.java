package dev.skidfuscator.obfuscator.util.progress.components;

import lukfor.progress.renderer.labels.TaskNameLabel;
import lukfor.progress.tasks.monitors.TaskMonitor;

public class SkidTaskNameLabel extends TaskNameLabel {
    @Override
    public void render(TaskMonitor monitor, StringBuilder buffer) {
        if (monitor.isDone())
            return;

        super.render(monitor, buffer);
    }
}
