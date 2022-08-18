package dev.skidfuscator.obfuscator.util.progress.components;

import lukfor.progress.renderer.labels.StringLabel;
import lukfor.progress.tasks.monitors.TaskMonitor;

public class SkidSpace extends StringLabel {
    public SkidSpace() {
        super(" ");
    }

    @Override
    public void render(TaskMonitor monitor, StringBuilder buffer) {
        if (monitor.isDone())
            return;

        super.render(monitor, buffer);
    }
}
