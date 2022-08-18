package dev.skidfuscator.obfuscator.util.progress.components;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SComponents {
    public SkidSpinner SPINNER = new SkidSpinner();
    public SkidTask FINISH = new SkidTask();

    public SkidRamLabel RAM = new SkidRamLabel();

    public SkidDefaultLabel PROGRESS_LABEL = new SkidDefaultLabel();

    public SkidDefaultProgressBar PROGRESS_BAR = new SkidDefaultProgressBar();

    public SkidTaskNameLabel TASK_NAME = new SkidTaskNameLabel();

    public SkidTimeLabel TIME = new SkidTimeLabel();

    public SkidSpace SPACE = new SkidSpace();
}
