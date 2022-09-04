package dev.skidfuscator.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.collections.LazilyInitializedFileCollection;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.jvm.internal.JvmEcosystemUtilities;

public class SkidfuscatorRuntime {

    private static final String NOTATION = "dev.skidfuscator.community:obfuscator:%s";

    private final ProjectInternal project;
    private final String version;

    public SkidfuscatorRuntime(Project project, String version) {
        this.project = (ProjectInternal) project;
        this.version = version;
    }

    public FileCollection fetchClasspath() {
        return new LazilyInitializedFileCollection() {
            @Override
            public String getDisplayName() {
                return "Skidobfuscator runtime classpath";
            }

            @Override
            public FileCollection createDelegate() {
                Dependency dependency = project.getDependencies().create(String.format(NOTATION, version));

                return detachedRuntimeClasspath(dependency);
            }

            private Configuration detachedRuntimeClasspath(Dependency... dependencies) {
                Configuration classpath = project.getConfigurations().detachedConfiguration(dependencies);
                jvmEcosystemUtilities().configureAsRuntimeClasspath(classpath);
                return classpath;
            }
        };
    }

    private JvmEcosystemUtilities jvmEcosystemUtilities() {
        return project.getServices().get(JvmEcosystemUtilities.class);
    }

}
