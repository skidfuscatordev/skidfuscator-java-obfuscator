package dev.skidfuscator.gradle;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import org.gradle.api.Action;
import org.gradle.api.Task;

import javax.inject.Inject;
import java.util.List;

public class SkidfuscatorCompileAction implements Action<Task> {

    private final SkidfuscatorSession session;
    private final List<String> excludes;

    @Inject
    public SkidfuscatorCompileAction(SkidfuscatorSession session, List<String> exemptionString) {
        this.session = session;
        this.excludes = exemptionString;
    }

    @Override
    public void execute(Task task) {
        Skidfuscator skidfuscator = new Skidfuscator(this.session);
        this.excludes.forEach(exclude -> skidfuscator.getExemptAnalysis().add(exclude));
        skidfuscator.run();
    }
}
