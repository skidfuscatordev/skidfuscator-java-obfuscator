package dev.skidfuscator.obfuscator.util.progress.components;

import lukfor.progress.renderer.labels.DefaultLabel;
import lukfor.progress.tasks.monitors.TaskMonitor;

public class SkidDefaultLabel extends DefaultLabel {
    @Override
    public void render(TaskMonitor monitor, StringBuilder buffer) {
        if (monitor.isDone())
            return;

        super.render(monitor, buffer);
    }
}
