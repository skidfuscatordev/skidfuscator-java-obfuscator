package dev.skidfuscator.obfuscator.util.progress;

import java.io.PrintStream;
import java.util.function.Consumer;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.util.progress.renderer.SkidAnimatedProgressRenderer;
import dev.skidfuscator.obfuscator.util.progress.renderer.SkidRendererThread;
import lukfor.progress.renderer.*;
import lukfor.progress.tasks.TaskFailureStrategy;
import lukfor.progress.tasks.monitors.TaskMonitor;
import lukfor.progress.util.AnsiColors;

public class TaskServiceBuilder {

	private boolean animated = true;
	private PrintStream target = System.out;
	private IProgressIndicator[] components = null;
	private TaskFailureStrategy taskFailureStrategy;
	private String name;
	private int count;
	private Consumer<AbstractProgressRenderer> callback;

	public TaskServiceBuilder() {

	}

	public TaskServiceBuilder style(IProgressIndicator... components) {
		this.components = components;
		return this;
	}

	public TaskServiceBuilder animated(boolean animated) {
		this.animated = animated;
		return this;
	}

	public TaskServiceBuilder target(PrintStream target) {
		this.target = target;
		return this;
	}

	public TaskServiceBuilder onFailure(TaskFailureStrategy taskFailureStrategy) {
		this.taskFailureStrategy = taskFailureStrategy;
		return this;
	}

	public TaskServiceBuilder count(int counts) {
		this.count = counts;
		return this;
	}

	public TaskServiceBuilder name(String name) {
		this.name = name;
		return this;
	}

	public TaskServiceBuilder callback(Consumer<AbstractProgressRenderer> callback) {
		this.callback = callback;
		return this;
	}

	public SkidProgressBar build() {

		AbstractProgressRenderer renderer = null;

		if (animated) {
			renderer = new SkidAnimatedProgressRenderer();
		} else {
			renderer = new StaticProgressRenderer();
		}

		if (components != null) {
			renderer.setComponents(components);
		}
		AnsiColors.enable();
		renderer.setTarget(target);
		renderer.setTaskFailureStrategy(taskFailureStrategy);

		final TaskMonitor monitor = new TaskMonitor();
		monitor.begin(name, count);
		monitor.setRenderer(renderer);
		monitor.start();

		renderer.addTaskMonitor(monitor);

		RendererThread.FRAME_RATE = 1.F / 9.F;
		// start renderer thread only when animated
		final AbstractProgressRenderer finalRenderer = renderer;
		Skidfuscator.LOGGER.post(" ");
		new Thread(new SkidRendererThread(finalRenderer, callback)).start();

		return new SkidProgressBar(monitor);
	}

}