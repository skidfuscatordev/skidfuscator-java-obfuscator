package dev.skidfuscator.obfuscator.util.progress.components;

import lukfor.progress.renderer.IProgressIndicator;
import lukfor.progress.tasks.monitors.TaskMonitor;
import lukfor.progress.util.AnsiColors;

public class SkidSpinner implements IProgressIndicator {

	public static final float FRAME_RATE = 1 / 100F;

	public static final String SEQUENCE = "⠁⠁⠉⠙⠚⠒⠂⠂⠒⠲⠴⠤⠄⠄⠤⠠⠠⠤⠦⠖⠒⠐⠐⠒⠓⠋⠉⠈⠈ ";

	@Override
	public void render(TaskMonitor monitor, StringBuilder buffer) {
		if (monitor.isDone()) {
			return;
		}

		int frame = getFrame(monitor, FRAME_RATE) % SEQUENCE.length();
		buffer.append(AnsiColors.cyan(" " + SEQUENCE.charAt(frame) + " "));
	}

	public int getFrame(TaskMonitor monitor, float frameRate) {
		return (int) (monitor.getExecutionTime() * frameRate);
	}

}