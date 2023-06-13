package dev.skidfuscator.obfuscator.util.progress.components;

import lukfor.progress.renderer.IProgressIndicator;
import lukfor.progress.tasks.monitors.TaskMonitor;
import lukfor.progress.util.TimeUtil;

public class SkidTaskCustom implements IProgressIndicator {
	private final String render;

	public SkidTaskCustom(String render) {
		this.render = render;
	}

	@Override
	public void render(TaskMonitor monitor, StringBuilder buffer) {
		if (!monitor.isDone())
			return;

		buffer.append(
				render.replace(
				"%%__TIME__%%", TimeUtil.format(monitor.getExecutionTime())
				)
		);
	}
}