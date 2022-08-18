package dev.skidfuscator.obfuscator.util.progress.components;

import lukfor.progress.renderer.IProgressIndicator;
import lukfor.progress.tasks.monitors.TaskMonitor;
import lukfor.progress.util.TimeUtil;

public class SkidTask implements IProgressIndicator {
	@Override
	public void render(TaskMonitor monitor, StringBuilder buffer) {
		if (!monitor.isDone())
			return;

		buffer.append("✨ Done in ").append(TimeUtil.format(monitor.getExecutionTime())).append("                                                      ");
	}
}