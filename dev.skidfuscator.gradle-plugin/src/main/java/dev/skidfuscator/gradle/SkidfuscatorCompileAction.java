package dev.skidfuscator.gradle;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.compile.AbstractCompile;

@RequiredArgsConstructor
public class SkidfuscatorCompileAction implements Action<Task> {

    private final SkidfuscatorSession session;
    private final String exemptionString;

    @Override
    public void execute(Task task) {
        this.handleCompileTask((AbstractCompile) task);
    }

    public void handleCompileTask(AbstractCompile compile) {
        Skidfuscator skidfuscator = new Skidfuscator(this.session);
        if (this.exemptionString != null)
            skidfuscator.getExemptAnalysis().add(this.exemptionString);
        skidfuscator.run();
    }
}
