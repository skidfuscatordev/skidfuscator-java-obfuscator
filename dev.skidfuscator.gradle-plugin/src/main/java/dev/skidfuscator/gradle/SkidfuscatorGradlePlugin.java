package dev.skidfuscator.gradle;

import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.scala.ScalaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SkidfuscatorGradlePlugin implements Plugin<Project> {

    private List<File> classpath;
    private SourceSetContainer sourceSets;
    private int jvm;

    @Override
    public void apply(Project project) {
        SkidfuscatorExtension extension = project.getExtensions().create("skidfuscator", SkidfuscatorExtension.class);

        this.sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        this.classpath = new ArrayList<>();
        this.jvm = 8;
        project.afterEvaluate(p -> this.handle(p, extension));
    }

    private void handle(Project project, SkidfuscatorExtension extension) {
        // read jvm and classpath information directly from language's compile task
        project.getPlugins().withType(JavaPlugin.class, plugin -> this.handlePlugin(project, "java"));
        project.getPlugins().withType(GroovyPlugin.class, plugin -> this.handlePlugin(project, "groovy"));
        project.getPlugins().withType(ScalaPlugin.class, plugin -> this.handlePlugin(project, "scala"));
        project.getPlugins().withId("org.jetbrains.kotlin.jvm", plugin -> this.handlePlugin(project, "kotlin"));
        // compute session and obfuscator right after jar task
        project.getTasks().withType(Jar.class, jar -> {
            final String home = System.getProperty("java.home");
            final File runtime;
            final File exemption;

            if (extension.getRuntime().isPresent()) {
                runtime = extension.getRuntime().get().getAsFile();
            } else {
                runtime = new File(
                        home,
                        MiscUtil.getJavaVersion() > 8
                                ? "jmods"
                                : "lib/rt.jar"
                );
            }

            if (extension.getExemption().isPresent()) {
                exemption = extension.getExemption().get().getAsFile();;
            } else {
                exemption = null;
            }

            File input = jar.getArchiveFile().get().getAsFile();
            File directory = input.getParentFile();

            File output = new File(directory, FilenameUtils.getName(input.getName()) + extension.getClassifier().get() + FilenameUtils.getExtension(input.getName()));
            SkidfuscatorSession session = SkidfuscatorSession.builder()
                    .input(input)
                    .output(output)
                    .fuckit(extension.getFuckit().get())
                    .phantom(extension.getPhantom().get())
                    .analytics(!extension.getNotrack().get())
                    .runtime(runtime)
                    .exempt(exemption)
                    .jmod(this.jvm > 8)
                    .libs(this.classpath.toArray(new File[0]))
                    .build();

            SkidfuscatorCompileAction action = project.getObjects().newInstance(SkidfuscatorCompileAction.class, session, extension.getExemptionString().getOrNull());
            jar.doLast("skidfuscator", action);
        });
    }

    private void handlePlugin(Project project, String language) {
        sourceSets.all(sourceSet -> {
            project.getTasks().named(sourceSet.getCompileTaskName(language), AbstractCompile.class, compile -> {
                this.jvm = MiscUtil.decodeJvmVersion(compile.getTargetCompatibility());
                this.classpath.addAll(compile.getClasspath().getFiles());
            });
        });
    }

}
