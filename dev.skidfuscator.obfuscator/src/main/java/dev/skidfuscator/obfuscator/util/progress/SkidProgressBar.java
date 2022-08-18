package dev.skidfuscator.obfuscator.util.progress;

import lukfor.progress.tasks.monitors.TaskMonitor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkidProgressBar implements ProgressWrapper {
    public static final ExecutorService RENDER_THREAD = Executors.newSingleThreadExecutor();
    private final TaskMonitor monitor;

    public SkidProgressBar(TaskMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void tick() {
        monitor.worked(1);
    }

    @Override
    public void tick(int amount) {
        monitor.worked(amount);
    }

    @Override
    public void succeed() {
        monitor.setSuccess(true);
    }

    @Override
    public void fail(final Throwable e) {
        monitor.setSuccess(false);
        monitor.setThrowable(e);
    }

    @Override
    public void close() {
        monitor.done();
        ProgressWrapper.super.close();
    }
}
